package snownee.jade.gui;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import snownee.jade.Jade;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.util.ModIdentification;
import snownee.jade.util.SmoothChasingValue;

public class HomeConfigScreen extends GuiScreen {

	private final Random random = new Random(42);
	private final @Nullable GuiScreen parent;
	private final SmoothChasingValue titleY;
	private final List<TextParticle> particles = Lists.newArrayList();
	private final List<TextParticle> pendingParticles = Lists.newArrayList();
	private final List<TextParticle> persistentParticles = Lists.newArrayList();
	private float ticks;
	private byte festival;
	private float nextParticleIn;
	private boolean showTranslators;
	private int lastMouseX;
	private int lastMouseY;

	public HomeConfigScreen(@Nullable GuiScreen parent) {
		this.parent = parent;
		titleY = new SmoothChasingValue().start(8).target(32).withSpeed(0.1F);

		LocalDate now = LocalDate.now();
		int month = now.getMonthValue();
		int day = now.getDayOfMonth();
		if (month == 12 && day >= 24 && day <= 26) {
			festival = 1;
		} else if (month == 6 && (day == 1 || day == 28)) {
			festival = 2;
		} else if (month <= 2 && isLunarNewYear(now)) {
			festival = 99;
		}
	}

	private static boolean isLunarNewYear(LocalDate now) {
		int year = now.getYear();
		int newYearMonthAndDay;
		switch (year) {
			case 2026 -> newYearMonthAndDay = 217;
			case 2027 -> newYearMonthAndDay = 206;
			case 2028 -> newYearMonthAndDay = 126;
			case 2029 -> newYearMonthAndDay = 213;
			case 2030 -> newYearMonthAndDay = 203;
			case 2031 -> newYearMonthAndDay = 123;
			case 2032 -> newYearMonthAndDay = 211;
			case 2033 -> newYearMonthAndDay = 131;
			case 2034 -> newYearMonthAndDay = 219;
			case 2035 -> newYearMonthAndDay = 208;
			case 2036 -> newYearMonthAndDay = 128;
			case 2037 -> newYearMonthAndDay = 215;
			case 2038 -> newYearMonthAndDay = 204;
			case 2039 -> newYearMonthAndDay = 124;
			case 2040 -> newYearMonthAndDay = 212;
			case 2041 -> newYearMonthAndDay = 201;
			case 2042 -> newYearMonthAndDay = 122;
			case 2043 -> newYearMonthAndDay = 210;
			default -> newYearMonthAndDay = 0;
		}
		if (newYearMonthAndDay == 0) {
			return false;
		}
		int newYearMonth = newYearMonthAndDay / 100;
		int newYearDay = newYearMonthAndDay % 100;
		LocalDate newYearDate = LocalDate.of(year, newYearMonth, newYearDay);
		int newYearDayOfYear = newYearDate.getDayOfYear();
		int dayOfYear = now.getDayOfYear();
		return dayOfYear >= newYearDayOfYear - 1 && dayOfYear <= newYearDayOfYear + 2;
	}

	@Override
	public void initGui() {
		Objects.requireNonNull(mc);
		particles.clear();
		ITextComponent modSettings = new TextComponentTranslation("gui.jade.jade_settings");
		ITextComponent pluginSettings = new TextComponentTranslation("gui.jade.plugin_settings");
		ITextComponent profileSettings = new TextComponentTranslation("gui.jade.profile_settings");
		int maxWidth = Math.max(100, Math.max(mc.fontRenderer.getStringWidth(modSettings.getFormattedText()) + 8,
				mc.fontRenderer.getStringWidth(pluginSettings.getFormattedText()) + 8));
		maxWidth = Math.min(maxWidth, Math.min(240, width / 2 - 40));

		buttonList.add(new GuiButton(0, width / 2 - 5 - maxWidth, height / 2 - 10, maxWidth, 20, modSettings.getFormattedText()) {
			@Override
			public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
				if (super.mousePressed(mc, mouseX, mouseY)) {
					visitedChildScreen();
					mc.displayGuiScreen(new WailaConfigScreen(HomeConfigScreen.this));
					return true;
				}
				return false;
			}
		});
		buttonList.add(new GuiButton(1, width / 2 + 5, height / 2 - 10, maxWidth, 20, pluginSettings.getFormattedText()) {
			@Override
			public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
				if (super.mousePressed(mc, mouseX, mouseY)) {
					visitedChildScreen();
					mc.displayGuiScreen(new PluginsConfigScreen(HomeConfigScreen.this));
					return true;
				}
				return false;
			}
		});
		// 1.12.2: no profile sprite texture exists, so the profile button is a plain 20x20 button
		buttonList.add(new GuiButton(2, width / 2 + 10 + maxWidth, height / 2 - 10, 20, 20, "☰") {
			@Override
			public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
				if (super.mousePressed(mc, mouseX, mouseY)) {
					visitedChildScreen();
					mc.displayGuiScreen(new ProfileConfigScreen(HomeConfigScreen.this));
					return true;
				}
				return false;
			}
		});
		buttonList.add(new GuiButton(3, width / 2 - 50, height / 2 + 20, 100, 20, I18n.format("gui.done")) {
			@Override
			public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
				if (super.mousePressed(mc, mouseX, mouseY)) {
					onClose();
					return true;
				}
				return false;
			}
		});

		Style style = new Style();
		if (festival != 0 && festival != 1) {
			style.setColor(TextFormatting.GOLD);
		}
		ITextComponent title = new TextComponentTranslation("gui.jade.by",
				new TextComponentString("❤").setStyle(new Style().setColor(TextFormatting.RED))).setStyle(style);
		ITextComponent hoveredTitle = new TextComponentTranslation("gui.jade.by.hovered").setStyle(style);
		int btnWidth = mc.fontRenderer.getStringWidth(title.getFormattedText());
		int btnX = (int) (width * 0.5F - btnWidth * 0.5F);
		int btnY = (int) (height * 0.9F - 5);
		CreditButton creditButton = new CreditButton(
				btnX,
				btnY,
				btnWidth,
				10,
				title,
				hoveredTitle,
				b -> mc.displayGuiScreen(new GuiConfirmOpenLink(
						new GuiYesNoCallbackImpl("https://www.curseforge.com/members/snownee_/projects"),
						"https://www.curseforge.com/members/snownee_/projects",
						13,
						false)),
				this::triggerAuthorButton);
		buttonList.add(creditButton);
		if (showTranslators) {
			creditButton.showTranslators();
		}
	}

	/** 1.12.2: GuiConfirmOpenLink hands the result back to the parent screen's confirmClicked. */
	private class GuiYesNoCallbackImpl implements GuiYesNoCallback {
		private final String link;

		GuiYesNoCallbackImpl(String link) {
			this.link = link;
		}

		@Override
		public void confirmClicked(boolean result, int id) {
			// 1.12.2: GuiConfirmOpenLink keeps the confirm screen on screen after the callback;
			// return to the settings screen explicitly (the modern ConfirmLinkScreen pops back to
			// its parent after opening/copying).
			if (id == 13) {
				if (result) {
					openWebLink(link);
				}
				mc.displayGuiScreen(HomeConfigScreen.this);
			}
		}
	}

	/** 1.12.2: GuiScreen.openWebLink is private, so the URI is opened through the OS desktop. */
	private void openWebLink(String link) {
		try {
			java.awt.Desktop.getDesktop().browse(java.net.URI.create(link));
		} catch (Exception e) {
			Jade.LOGGER.error("Failed to open link %s".formatted(link), e);
		}
	}

	private void visitedChildScreen() {
		titleY.set(titleY.getTarget());
		showTranslators = true;
	}

	private void triggerAuthorButton(CreditButton button) {
		if (festival == 2 || festival == 3) {
			festival = 3;
			return;
		}
		IntList colors = new IntArrayList();
		String text = "❄";
		if (festival == 99) {
			for (int i = 0; i < 11; i++) {
				colors.add(random.nextBoolean() ? 0xA80000 : 0xC01800);
			}
			text = "✐";
		} else {
			for (int i = 0; i < 11; i++) {
				colors.add(colorFromFloat(1, 1 - random.nextFloat() * 0.6F, 1, 1));
			}
		}
		for (int color : colors) {
			int ox = nextIntBetweenInclusive(-button.width / 2, button.width / 2);
			float x = width * 0.5F + ox;
			float y = nextIntBetweenInclusive(button.y, button.y + button.height);
			float dx = ox * 0.08F;
			float dy = -5 - random.nextFloat() * 3;
			var particle = new TextParticle(text, x, y, dx, dy, color, 0.75F + random.nextFloat() * 0.5F);
			particles.add(particle);
			if (festival == 99) {
				particle.age = 8 + random.nextFloat() * 5;
			}
		}
	}

	private int nextIntBetweenInclusive(int min, int max) {
		return min + random.nextInt(max - min + 1);
	}

	/** {@code ARGB.colorFromFloat} replacement. */
	private static int colorFromFloat(float a, float r, float g, float b) {
		return ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}

	/**
	 * 1.12.2: onGuiClosed fires on every screen switch, so it must not save here (the done
	 * button's onClose saves, then displayGuiScreen(parent) fires this again); the cancel/ESC
	 * path is onClose only. Mirrors BaseOptionsScreen.
	 */
	@Override
	public void onGuiClosed() {
	}

	/**
	 * 1.12.2: no onClose() in GuiScreen; the done button calls this directly (mirrors the modern onClose flow:
	 * save, reload ignore lists, return to the parent screen).
	 */
	public void onClose() {
		Objects.requireNonNull(mc);
		IWailaConfig.get().save();
		WailaClientRegistration.instance().reloadIgnoreLists();
		if (parent != null) {
			mc.displayGuiScreen(parent);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		Objects.requireNonNull(mc);
		ticks += partialTicks;
		particle:
		if (ticks > nextParticleIn) {
			if (festival == 3) {
				nextParticleIn = ticks;
				if (pendingParticles.isEmpty()) {
					festival3populateNew();
				}
				if (Math.abs(mouseX - lastMouseX) < 3 && Math.abs(mouseY - lastMouseY) < 3) {
					break particle;
				}
				lastMouseX = mouseX;
				lastMouseY = mouseY;
				TextParticle particle = pendingParticles.remove(0);
				particle.x = mouseX;
				particle.y = mouseY;
				particle.gravity = 0F;
				particle.age = 40;
				particles.add(particle);
				persistentParticles.add(particle);
				if (persistentParticles.size() > 50) {
					TextParticle first = persistentParticles.remove(0);
					first.persistent = false;
				}
			} else if (festival == 1) {
				nextParticleIn = ticks + 10 + random.nextFloat() * 10;
				int color = colorFromFloat(1, 1 - random.nextFloat() * 0.6F, 1, 1);
				color |= (random.nextInt(80) + 40) << 24;
				int x = nextIntBetweenInclusive(40, width + 100);
				var particle = new TextParticle("❄", x, -20, -0.3F, 0.5f, color, 2F + random.nextFloat());
				particle.gravity = 0F;
				particles.add(particle);
			}
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
		int left = width / 2 - 105;
		int top = height / 4 - 20;
		GlStateManager.pushMatrix();
		GlStateManager.translate(left, top, 0);

		float scale = 2F;
		GlStateManager.scale(scale, scale, 1);
		mc.fontRenderer.drawString(ModIdentification.getModFullName(Jade.ID).orElse("Jade"), 0, 0, 0xFFFFFFFF);

		GlStateManager.scale(0.5F, 0.5F, 1);
		titleY.tick(partialTicks);
		String desc2 = I18n.format("gui.jade.configuration.desc2");
		float scaledX, scaledY;
		if (desc2.isEmpty()) {
			GlStateManager.popMatrix();
			GlStateManager.pushMatrix();
			GlStateManager.translate(left, top, 0);
			scaledX = mouseX - left;
			scaledY = mouseY - top;
		} else {
			scaledX = (mouseX - left) / scale * 2;
			scaledY = (mouseY - top) / scale * 2;
		}
		drawFancyTitle(I18n.format("gui.jade.configuration.desc1"), Math.min(titleY.value, 20F), 20F, scaledX, scaledY);
		if (!desc2.isEmpty()) {
			drawFancyTitle(desc2, Math.min(titleY.value + 3F, 32F), 32F, scaledX, scaledY);
		}
		GlStateManager.popMatrix();

		particles.removeIf(p -> {
			p.tick(partialTicks);
			if (p.y > height + 20) {
				return true;
			}
			p.render(mouseX, mouseY);
			return false;
		});
	}

	private void festival3populateNew() {
		IntList colors = new IntArrayList();
		String text = random.nextBoolean() ? "UwU" : "OwO";
		switch (random.nextInt(7)) {
			case 0 -> {
				colors.add(0xE40303);
				colors.add(0xFF8C00);
				colors.add(0xFFED00);
				colors.add(0x008026);
				colors.add(0x732982);
				colors.add(0x732982);
			}
			case 1 -> {
				colors.add(0x5BCEFA);
				colors.add(0xF5A9B8);
				colors.add(0xFFFFFF);
				colors.add(0xF5A9B8);
				colors.add(0x5BCEFA);
			}
			case 2 -> {
				colors.add(0xD60270);
				colors.add(0xD60270);
				colors.add(0x9B4F96);
				colors.add(0x0038A8);
				colors.add(0x0038A8);
			}
			case 3 -> {
				colors.add(0xFF218C);
				colors.add(0xFF218C);
				colors.add(0xFFD800);
				colors.add(0xFFD800);
				colors.add(0x21B1FF);
				colors.add(0x21B1FF);
			}
			case 4 -> {
				colors.add(0x000000);
				colors.add(0xA3A3A3);
				colors.add(0xFFFFFF);
				colors.add(0x800080);
			}
			case 5 -> {
				colors.add(0xFF76A4);
				colors.add(0xFFFFFF);
				colors.add(0xC011D7);
				colors.add(0x000000);
				colors.add(0x2F3CBE);
			}
			case 6 -> {
				colors.add(0xFCF434);
				colors.add(0xFFFFFF);
				colors.add(0x9C59D1);
				colors.add(0x2C2C2C);
			}
			default -> {
			}
		}
		for (int color : colors) {
			for (int i = 0; i < 5; i++) {
				float rot = random.nextFloat() * (float) (Math.PI * 2);
				float dx = MathHelper.cos(rot) * 2;
				float dy = MathHelper.sin(rot) * 2;
				var particle = new TextParticle(text, 0, 0, dx, dy, color | 0xFF000000, 1);
				pendingParticles.add(particle);
			}
		}
	}

	private void drawFancyTitle(String text, float y, float expectY, float mouseX, float mouseY) {
		float distY = Math.abs(y - expectY);
		if (distY >= 9) {
			return;
		}
		int color = IWailaConfig.Overlay.applyAlpha(0xFFAAAAAA, 1 - distY / 10F);
		float glint1 = (ticks - y / 5F) % 90 / 45 * width;
		float glint2 = mouseX;
		float glint1Strength = 1;
		float glint2Strength = 1 - MathHelper.clamp(Math.abs(mouseY - y) / 20F, 0, 1);

		// 1.12.2: no §x hex-color format codes (the FontRenderer format-code parser clamps
		// 'x' to -1), so the per-glyph glint color is applied by drawing each glyph
		// separately instead of baking §x codes into a single string.
		int x = 0;
		GlStateManager.pushMatrix();
		GlStateManager.translate(0, y, 0);
		for (int i = 0; i < text.length(); ) {
			int codePoint = text.codePointAt(i);
			i += Character.charCount(codePoint);
			String s = new String(Character.toChars(codePoint));
			int glyphWidth = mc.fontRenderer.getStringWidth(s);
			x += glyphWidth;
			int curXVal = x + glyphWidth / 2;
			float dist = Math.abs(curXVal - glint1);
			float localGlint1 = 0.65F + MathHelper.clamp(1 - dist / 20, 0, 1) * 0.35F * glint1Strength;
			dist = Math.abs(curXVal - glint2);
			float localGlint2 = 0.65F + MathHelper.clamp(1 - dist / 20, 0, 1) * 0.35F * glint2Strength;
			float colorMul = Math.max(localGlint1, localGlint2);
			int originalColor = 0xAAAAAA;
			float colorMulClamped = Math.max(0, Math.min(1, colorMul));
			int r = (int) (((originalColor >> 16) & 0xFF) * colorMulClamped);
			int g = (int) (((originalColor >> 8) & 0xFF) * colorMulClamped);
			int b = (int) ((originalColor & 0xFF) * colorMulClamped);
			mc.fontRenderer.drawString(s, x - glyphWidth, 0, (color & 0xFF000000) | (r << 16) | (g << 8) | b);
		}
		GlStateManager.popMatrix();
	}

	private class TextParticle {
		private float age;
		private String text;
		private float x;
		private float y;
		private float motionX;
		private float motionY;
		private int color;
		private float scale;
		private float gravity = 0.98F;
		private boolean persistent = true;
		private float fade = 1;

		public TextParticle(String text, float x, float y, float motionX, float motionY, int color, float scale) {
			this.text = text;
			this.x = x;
			this.y = y;
			this.motionX = motionX;
			this.motionY = motionY;
			this.color = color;
			this.scale = scale;
		}

		private void tick(float partialTicks) {
			x += motionX * partialTicks;
			y += motionY * partialTicks;
			motionY += gravity * partialTicks;
			if (festival == 3) {
				motionX *= 0.88F;
				motionY *= 0.88F;
				if (age < 0) {
					persistent = false;
				}
			}
			boolean greaterThanZero = age > 0;
			age -= partialTicks;
			if (festival == 99) {
				if (greaterThanZero && age <= 0) {
					text = random.nextBoolean() ? "✴" : "✳";
					color = random.nextBoolean() ? 0xFFD427 : 0xF0C415;
					Objects.requireNonNull(mc);
					mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
							random.nextBoolean() ?
									SoundEvents.ENTITY_FIREWORK_BLAST :
									SoundEvents.ENTITY_FIREWORK_LARGE_BLAST, 0.7F));
				}
			}
			if (!persistent) {
				fade = Math.max(0, fade - partialTicks * 0.25F);
			}
		}

		private void render(int mouseX, int mouseY) {
			if (festival == 99 && age < -4) {
				return;
			}
			GlStateManager.pushMatrix();
			GlStateManager.translate(x, y, 0);
			GlStateManager.scale(scale, scale, 1);
			int color = this.color;
			if (festival == 1) {
				GlStateManager.rotate(age / 50, 0, 0, 1);
				float alpha = MathHelper.clamp((Math.abs(mouseX - x) + Math.abs(mouseY - y)) / 50F, 0.25F, 1);
				color = IWailaConfig.Overlay.applyAlpha(color, alpha);
			} else if (fade != 1) {
				color = IWailaConfig.Overlay.applyAlpha(color, fade);
			}
			mc.fontRenderer.drawString(text, 0, 0, color);
			GlStateManager.popMatrix();
		}
	}

}
