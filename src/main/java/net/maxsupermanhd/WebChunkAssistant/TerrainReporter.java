package net.maxsupermanhd.WebChunkAssistant;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.LiteralText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.item.Items.ELYTRA;
import static net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;

public class TerrainReporter {
	public static final Logger LOGGER = LogManager.getLogger("TerrainReporter");
	public static class ChunkSender implements Runnable {
		public String status = "Allocated";
		public boolean isFinished = false;
		private final long allocatedAt;
		private final int x;
		private final int z;
		public ChunkSender(int x, int z, long t) {
			this.allocatedAt = t;
			this.x = x;
			this.z = z;
		}
		private void addDebugMessage(MinecraftClient mc, String pattern, Object ... args) {
			mc.inGameHud.getChatHud().addMessage(new LiteralText("").append(MessageFormat.format(pattern, args)));
		}
		private void waitAndDrop(int ms) {
			try {
				Thread.sleep(ms);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			TerrainReporter.SendingChunks.remove(allocatedAt);
		}
		public void run() {
			MinecraftClient mc = MinecraftClient.getInstance();
			try {
				status = "Started";
				LOGGER.info("Loaded chunk {} {}", x, z);
				ChunkAssistantConfig config = AutoConfig.getConfigHolder(ChunkAssistantConfig.class).getConfig();
				if(mc.world == null) {
					throw new Exception(String.format("Failed to submit chunk %d:%d because world is null", x, z));
				}
				status = "Serializing";
				NbtCompound body = ClientChunkSerializer.serializeClientChunk(mc.world, x, z);
				URL url;
				String subWorld = config.submitWorld;
				if(subWorld.isEmpty()) {
					var e = mc.getCurrentServerEntry();
					if(e == null) {
						throw new Exception("server entry null and no world name set");
					} else {
						subWorld = e.address;
					}
				}
				String subDim = config.submitDimension;
				if(subDim.isEmpty()) {
					subDim = mc.world.getRegistryKey().getValue().toString().replace("minecraft:", "");
				}
				url = new URL(String.format(config.submiturl, subDim, subWorld,	x, z));
				HttpURLConnection con;
				status = "Connecting";
				con = (HttpURLConnection)url.openConnection();
				if(con == null) {
					this.addDebugMessage(mc, "Con is null");
					return;
				}
				con.setRequestMethod("POST");
				con.setRequestProperty("Content-Type", "binary/octet-stream");
				con.setRequestProperty("Accept", "text/plain");
				con.setDoOutput(true);
				status = "Writing";
				DataOutputStream o = new DataOutputStream(con.getOutputStream());
				o.write(1);
				NbtIo.writeCompressed(body, o);
				StringBuilder response = new StringBuilder();
				status = "Reading response";
				int httpstatus = con.getResponseCode();
				InputStream is;
				if(httpstatus != 200) {
//					throw new Exception(String.format("Server answered http %d - %s", httpstatus, response));
					is = con.getErrorStream();
				} else {
					is = con.getInputStream();
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
				String responseLine;
				while ((responseLine = br.readLine()) != null) {
					response.append(responseLine.trim());
				}
				status = response.toString();
				isFinished = true;
				waitAndDrop(1000);
			} catch(Exception e) {
				LOGGER.info(e);
				e.printStackTrace();
				this.addDebugMessage(mc, e.toString());
				waitAndDrop(3000);
			}
		}
	}

	public static ConcurrentHashMap<Long, ChunkSender> SendingChunks = new ConcurrentHashMap<>();

	public void RenderGameOverlayEvent(MatrixStack matrixStack, float delta) {
		ChunkAssistantConfig config = AutoConfig.getConfigHolder(ChunkAssistantConfig.class).getConfig();
		if(!config.render_overlay) {
			return;
		}
		TextRenderer r = MinecraftClient.getInstance().textRenderer;
		List<ChunkSender> valuesCopy = new ArrayList<>(SendingChunks.values());
		int finishedCount = 0;
		for (int i = 0; i < valuesCopy.size(); i++) {
			ChunkSender s = valuesCopy.get(i);
			if(!s.isFinished) {
				r.drawWithShadow(matrixStack, String.format("%d:%d %s", s.x, s.z, s.status),
						config.overlayX,	config.overlayY+(i-finishedCount+2)*config.overlayO,0xffffff);
			} else {
				finishedCount++;
			}
		}
		r.drawWithShadow(matrixStack, String.format("10s:%d 1m:%d", finishedCount, finishedCount*6),
				config.overlayX,	config.overlayY,0xffffff);
	}

	private int tickCounter = 0;

	public void clientTickEvent(MinecraftClient mc) {
		this.tickCounter++;
		if(this.tickCounter % 20 == 0 && mc.player != null) {
			ChunkAssistantConfig config = AutoConfig.getConfigHolder(ChunkAssistantConfig.class).getConfig();
			if(config.elytraWarning.lowElytraSoundEnabled) {
				ItemStack elytra = mc.player.getInventory().armor.get(2);
				if(elytra.isOf(ELYTRA) && elytra.getMaxDamage()-elytra.getDamage() < config.elytraWarning.lowElytraDurabilityLeft) {
					mc.getSoundManager().play(PositionedSoundInstance.master(ENTITY_EXPERIENCE_ORB_PICKUP, config.elytraWarning.lowElytraSoundPitch, config.elytraWarning.lowElytraSoundVolume));
					mc.getSoundManager().play(PositionedSoundInstance.master(ENTITY_EXPERIENCE_ORB_PICKUP, config.elytraWarning.lowElytraSoundPitch, config.elytraWarning.lowElytraSoundVolume), 4);
				}
			}
		}

	}

	public static void submitChunkTrigger(int x, int z) throws InterruptedException {
		LOGGER.info("Submitting chunk {}:{}", x, z);
		while(true) {
			long id = System.currentTimeMillis();
			//noinspection SuspiciousMethodCalls
			if(SendingChunks.contains(id)) {
				//noinspection BusyWait
				Thread.sleep(2);
				continue;
			}
			ChunkSender sender = new ChunkSender(x, z, id);
			SendingChunks.put(id, sender);
			new Thread(sender).start();
			break;
		}
	}
}
