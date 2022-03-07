package net.maxsupermanhd.TerrainReporter;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

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

public class TerrainReporter implements ModInitializer {
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
			TerrainReporterConfig config = AutoConfig.getConfigHolder(TerrainReporterConfig.class).getConfig();
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

	private void RenderGameOverlayEvent(MatrixStack matrixStack, float delta) {
		TerrainReporterConfig config = AutoConfig.getConfigHolder(TerrainReporterConfig.class).getConfig();
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

	public static final KeyBinding keybindOpenConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "Terrain reporter"));
	public static final KeyBinding keybindToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding("Toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "Terrain reporter"));
	public static final KeyBinding keybindToggleOverlay = KeyBindingHelper.registerKeyBinding(new KeyBinding("Toggle overlay", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "Terrain reporter"));
	@Override
	public void onInitialize() {
		AutoConfig.register(TerrainReporterConfig.class, JanksonConfigSerializer::new);
		LOGGER.info("Hello Fabric world!");
		HudRenderCallback.EVENT.register(this::RenderGameOverlayEvent);
		ClientTickEvents.END_CLIENT_TICK.register(this::clientTickEvent);
	}

	private int tickCounter = 0;

	private void clientTickEvent(MinecraftClient mc) {
		this.keyInputEvent(mc);
		this.tickCounter++;
		if(this.tickCounter % 20 == 0 && mc.player != null) {
			TerrainReporterConfig config = AutoConfig.getConfigHolder(TerrainReporterConfig.class).getConfig();
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

	private void keyInputEvent(MinecraftClient mc) {
		while(keybindOpenConfig.wasPressed()) {
			mc.setScreen(AutoConfig.getConfigScreen(TerrainReporterConfig.class, null).get());
		}
		while(keybindToggle.wasPressed()) {
			TerrainReporterConfig config = AutoConfig.getConfigHolder(TerrainReporterConfig.class).getConfig();
			config.enabled = !config.enabled;
			AutoConfig.getConfigHolder(TerrainReporterConfig.class).save();
		}
		while(keybindToggleOverlay.wasPressed()) {
			TerrainReporterConfig config = AutoConfig.getConfigHolder(TerrainReporterConfig.class).getConfig();
			config.render_overlay = !config.render_overlay;
			AutoConfig.getConfigHolder(TerrainReporterConfig.class).save();
		}
	}
}
