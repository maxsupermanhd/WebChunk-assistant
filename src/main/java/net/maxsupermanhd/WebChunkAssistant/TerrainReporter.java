package net.maxsupermanhd.WebChunkAssistant;

import net.maxsupermanhd.WebChunkAssistant.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
			mc.inGameHud.getChatHud().addMessage(((MutableText)Text.of("")).append(MessageFormat.format(pattern, args)));
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
				if(mc.world == null) {
					throw new Exception(String.format("Failed to submit chunk %d:%d because world is null", x, z));
				}
				status = "Serializing";
				NbtCompound body = ClientChunkSerializer.serializeClientChunk(mc.world, x, z);
				String subWorld = Config.Submission.OVERRIDE_WORLD.getValue();
				if(subWorld.isEmpty()) {
					var e = mc.getCurrentServerEntry();
					if(e == null) {
						throw new Exception("server entry null and no world name set");
					} else {
						subWorld = e.address;
					}
				}
				String subDim = Config.Submission.OVERRIDE_DIMENSION.getValue();
				if(subDim.isEmpty()) {
					subDim = mc.world.getRegistryKey().getValue().toString().replace("minecraft:", "");
				}
				var o = new ByteArrayOutputStream(265);
				o.write((byte)1);
				NbtIo.writeCompressed(body, o);
				status = "Connecting";
				String submiturl = Config.Server.BASE_URL.getValue() + Config.Server.ENDPOINT_SUBMIT.getValue();
				HttpRequest.Builder builder = HttpRequest.newBuilder().uri(new URL(String.format(submiturl, subDim, subWorld, x, z)).toURI());
				builder.header("Content-Type", "binary/octet-stream");
				builder.header("Accept", "text/plain, image/png, image/jpeg");
				builder.header("User-Agent", WebMapScreen.cachedUserAgent);
				builder.header("WebChunk-DrawTTYPE", "default");
				builder.method("POST", HttpRequest.BodyPublishers.ofByteArray(o.toByteArray()));
				builder.timeout(Duration.ofSeconds(8));
				status = "Writing";
				HttpResponse<byte[]> res = null;
				res = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
				status = "Reading response";
				int httpstatus = res.statusCode();
				if(httpstatus != 200) {
					throw new Exception("WebChunk responded with "+httpstatus+", and following bytes: "+ Arrays.toString(res.body()));
				}
				NativeImage img = NativeImage.read(ByteBuffer.wrap(res.body()));
				ChunkAssistant.mapScreen.updateTile(x, z, img);
				status = String.valueOf(res.statusCode());
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
		if(!Config.Overlay.ENABLED.getBooleanValue()) {
			return;
		}
		TextRenderer r = MinecraftClient.getInstance().textRenderer;
		List<ChunkSender> valuesCopy = new ArrayList<>(SendingChunks.values());
		int finishedCount = 0;
		int offsetX = Config.Overlay.OFFSET_X.getIntegerValue();
		int offsetY = Config.Overlay.OFFSET_Y.getIntegerValue();
		int spacing = Config.Overlay.LINE_SPACING.getIntegerValue();

		for (int i = 0; i < valuesCopy.size(); i++) {
			ChunkSender s = valuesCopy.get(i);
			if(!s.isFinished) {
				r.drawWithShadow(matrixStack, String.format("%d:%d %s", s.x, s.z, s.status),
						offsetX,	offsetY+(i-finishedCount+2)*spacing,0xffffff);
			} else {
				finishedCount++;
			}
		}
		r.drawWithShadow(matrixStack, String.format("10s:%d 1m:%d", finishedCount, finishedCount*6),
				offsetX, offsetY,0xffffff);
	}

	private int tickCounter = 0;

	public void clientTickEvent(MinecraftClient mc) {
//		this.tickCounter++;
//		if(this.tickCounter % 20 == 0 && mc.player != null) {
//			ChunkAssistantConfig config = AutoConfig.getConfigHolder(ChunkAssistantConfig.class).getConfig();
//			if(config.elytraWarning.lowElytraSoundEnabled) {
//				ItemStack elytra = mc.player.getInventory().armor.get(2);
//				if(elytra.isOf(ELYTRA) && elytra.getMaxDamage()-elytra.getDamage() < config.elytraWarning.lowElytraDurabilityLeft) {
//					mc.getSoundManager().play(PositionedSoundInstance.master(ENTITY_EXPERIENCE_ORB_PICKUP, config.elytraWarning.lowElytraSoundPitch, config.elytraWarning.lowElytraSoundVolume));
//					mc.getSoundManager().play(PositionedSoundInstance.master(ENTITY_EXPERIENCE_ORB_PICKUP, config.elytraWarning.lowElytraSoundPitch, config.elytraWarning.lowElytraSoundVolume), 4);
//				}
//			}
//		}
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
