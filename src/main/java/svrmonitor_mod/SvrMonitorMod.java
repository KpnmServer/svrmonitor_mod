
package com.github.kpnmserver.svrmonitor_mod;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.crash.CrashReport;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.github.kpnmserver.svrmonitor_mod.storage.Config;
import com.github.kpnmserver.svrmonitor_mod.connect.InfoUploader;
import com.github.kpnmserver.svrmonitor_mod.connect.HttpInfoUploader;
import com.github.kpnmserver.svrmonitor_mod.connect.WSInfoUploader;

public class SvrMonitorMod implements ModInitializer {
	public static SvrMonitorMod INSTANCE = null;
	public static final Logger LOGGER = LogManager.getLogger("SvrMonitor");

	private MinecraftServer server = null;
	private File folder;
	private volatile InfoUploader uploader;
	private Timer upload_helper = null;
	public volatile CrashReport crashreport = null;

	public SvrMonitorMod(){
		this.folder = new File("svrmonitor");
		if(!this.folder.exists()){
			this.folder.mkdirs();
		}
		INSTANCE = this;
	}

	public MinecraftServer getServer(){
		return this.server;
	}

	public File getDataFolder(){
		return this.folder;
	}

	@Override
	public void onInitialize(){
		LOGGER.info("SvrMonitor is onInitialize");
		ServerLifecycleEvents.SERVER_STARTING.register(this::onStarting);
		ServerLifecycleEvents.SERVER_STARTED.register(this::onStarted);
		CommandRegistrationCallback.EVENT.register(this::onRegisterCommands);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onStopping);
		ServerLifecycleEvents.SERVER_STOPPED.register(this::onStopped);
	}

	public void onStarting(MinecraftServer server){
		this.server = server;
		this.onReload();
		if(this.uploader != null){
			this.uploader.sendServerStatus(InfoUploader.SERVER_STARTING);
		}
	}

	public void onStarted(MinecraftServer server){
		if(this.uploader != null){
			this.uploader.sendServerStatus(InfoUploader.SERVER_STARTED);
		}
	}

	public void onRegisterCommands(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated){
		//TODO
	}

	public void onReload(){
		Config.INSTANCE.reload();
		if(this.upload_helper != null){
			this.upload_helper.cancel();
			this.upload_helper = null;
		}
		this.upload_helper = new Timer("svrmonitor-upload-helper", true);
		this.upload_helper.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run(){
				if(SvrMonitorMod.this.uploader != null && Config.INSTANCE.getEnable()){
					SvrMonitorMod.this.uploader.tick(server.getTicks());
				}
			}
		}, 100L, (long)(Config.INSTANCE.getUploadTime()) * 1000);
		if(this.uploader != null){
			this.uploader.sendServerStatus(InfoUploader.SERVER_UNKNOW);
			this.uploader.close();
			this.uploader = null;
		}
		if(Config.INSTANCE.getEnable()){
			try{
				final URI uploaduri = new URI(Config.INSTANCE.getUploadUrl());
				final String scheme = uploaduri.getScheme();
				if(scheme.equals("http") || scheme.equals("https")){
					this.uploader = new HttpInfoUploader(uploaduri);
				}else if(scheme.equals("ws") || scheme.equals("wss")){
					this.uploader = new WSInfoUploader(uploaduri);
				}else{
					LOGGER.error("Unknown scheme: " + scheme);
				}
			}catch(URISyntaxException | MalformedURLException e){
				LOGGER.error("Parse upload url error:\n", e);
			}
		}
	}

	public void onSave(){
		Config.INSTANCE.save();
	}

	public void onStopping(MinecraftServer server){
		if(this.uploader != null){
			this.uploader.sendServerStatus(InfoUploader.SERVER_STOPPING);
		}
		this.onSave();
	}

	public void onStopped(MinecraftServer server){
		if(this.uploader != null){
			this.uploader.sendServerStatus(InfoUploader.SERVER_STOPPED);
			if(this.crashreport == null){
				this.uploader.close();
			}else{
				this.uploader.close(this.crashreport.getCause());
			}
			this.uploader = null;
		}
		this.server = null;
	}
}
