
package com.github.kpnmserver.svrmonitor_mod.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.crash.CrashReport;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.kpnmserver.svrmonitor_mod.SvrMonitorMod;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
	@Inject(at=@At("HEAD"),
		method="setCrashReport(Lnet/minecraft/util/crash/CrashReport;)V")
	private void setCrashReport(CrashReport crashreport, CallbackInfo info){
		SvrMonitorMod.INSTANCE.crashreport = crashreport;
	}
}
