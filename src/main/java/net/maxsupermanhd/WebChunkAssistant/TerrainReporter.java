package net.maxsupermanhd.WebChunkAssistant;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ModInitializer;
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
import java.util.*;
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
			status = "Started";
			LOGGER.info("Loaded chunk {} {}", x, z);
			MinecraftClient mc = MinecraftClient.getInstance();
			ChunkAssistantConfig config = AutoConfig.getConfigHolder(ChunkAssistantConfig.class).getConfig();
			if(mc.world == null) {
				LOGGER.info("Failed to submit chunk {}:{} because world is null", x, z);
				this.addDebugMessage(mc, "Failed to submit chunk {}:{} because world is null", x, z);
				waitAndDrop(3000);
				return;
			}
			status = "Serializing";
			NbtCompound body = ClientChunkSerializer.serializeClientChunk(mc.world, x, z);
			URL url;
			try {
				url = new URL(String.format(
						config.baseurl,
						mc.world.getRegistryKey().getValue().toString().replace("minecraft:", ""),
						Objects.requireNonNull(mc.getCurrentServerEntry()).address,
						x, z));
			} catch (MalformedURLException e) {
				status = "wrong url man!";
				e.printStackTrace();
				this.addDebugMessage(mc, "Wrong URL! [{0}]", e.toString());
				waitAndDrop(3000);
				return;
			}
			HttpURLConnection con;
			try {
				status = "Connecting";
				con = (HttpURLConnection)url.openConnection();
			} catch (IOException e) {
				status = "Broke on connecting to server";
				this.addDebugMessage(mc, "Failed to connect to server! [{0}]", e.toString());
				e.printStackTrace();
				waitAndDrop(3000);
				return;
			}
			try {
				assert con != null;
				con.setRequestMethod("POST");
			} catch (ProtocolException e) {
				status = "Broke on setting protocol";
				e.printStackTrace();
				this.addDebugMessage(mc, "Failed to set request method! [{0}]", e.toString());
				waitAndDrop(3000);
				return;
			}
			con.setRequestProperty("Content-Type", "binary/octet-stream");
			con.setRequestProperty("Accept", "text/plain");
			con.setDoOutput(true);
			status = "Writing";
			try(OutputStream os = con.getOutputStream()) {
				DataOutputStream o = new DataOutputStream(os);
				o.write(1);
				NbtIo.writeCompressed(body, o);
			} catch (IOException e) {
				status = "Broke on sending chunk";
				e.printStackTrace();
				this.addDebugMessage(mc, "Failed to send chunk! [{0}]", e.toString());
				waitAndDrop(3000);
				return;
			}
			StringBuilder response = new StringBuilder();
			int httpstatus;
			status = "Reading response";
			try(BufferedReader br = new BufferedReader(
					new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
				String responseLine;
				while ((responseLine = br.readLine()) != null) {
					response.append(responseLine.trim());
				}
				LOGGER.info(response.toString());
				httpstatus = con.getResponseCode();
			} catch (IOException e) {
				status = "Broke on reading response";
				e.printStackTrace();
				this.addDebugMessage(mc, "Failed on reading response! [{0}]", e.toString());
				waitAndDrop(3000);
				return;
			}
			if(httpstatus != 200 && config.report_to_chat) {
				this.addDebugMessage(mc, "Server answered http {0} - {1}", httpstatus, response.toString());
			}
			status = "Done";
			isFinished = true;
			waitAndDrop(10000);
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
