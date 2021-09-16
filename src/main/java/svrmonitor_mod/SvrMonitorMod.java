
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
import net.minecraft.text.Text;
import net.minecraft.text.LiteralText;
import net.minecraft.util.crash.CrashReport;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.github.kpnmserver.svrmonitor_mod.command.SvrMonitorCommand;
import com.github.kpnmserver.svrmonitor_mod.storage.Config;
import com.github.kpnmserver.svrmonitor_mod.connect.InfoUploader;
import com.github.kpnmserver.svrmonitor_mod.connect.HttpInfoUploader;
import com.github.kpnmserver.svrmonitor_mod.connect.WSInfoUploader;

public class SvrMonitorMod implements ModInitializer {
	public static SvrMonitorMod INSTANCE = null;
	public static final Logger LOGGER = LogManager.getLogger("SvrMonitor");

	private MinecraftServer server = null;
	private File folder;
	private volatile InfoUploader uploader = null;
	private Timer upload_helper = null;
	private int statuscode = InfoUploader.SERVER_UNKNOWN;
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

	public int getStatusCode(){
		return this.statuscode;
	}

	public InfoUploader getUploader(){
		return this.uploader;
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
		this.statuscode = InfoUploader.SERVER_STARTING;
		if(this.uploader != null){
			this.uploader.sendServerStatus(InfoUploader.SERVER_STARTING);
		}
	}

	public void onStarted(MinecraftServer server){
		this.statuscode = InfoUploader.SERVER_STARTED;
		if(this.uploader != null){
			this.uploader.sendServerStatus(InfoUploader.SERVER_STARTED);
		}
	}

	public void onRegisterCommands(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated){
		SvrMonitorCommand.register(dispatcher);
	}

	public void onReload(){
		Config.INSTANCE.reload();
		if(this.upload_helper != null){
			this.upload_helper.cancel();
			this.upload_helper = null;
		}
		this.upload_helper = new Timer("svrmonitor-upload-helper", true);
		this.upload_helper.schedule(new TimerTask(){
			@Override
			public void run(){
				if(SvrMonitorMod.this.uploader != null && Config.INSTANCE.getEnable()){
					SvrMonitorMod.this.uploader.tick(SvrMonitorMod.this.server.getTicks());
				}
			}
		}, 100L, (long)(Config.INSTANCE.getUploadTime()) * 1000);
		this.stopUploader();
		try{
			this.startUploader();
		}catch(Exception e){
			LOGGER.error(e.getMessage());
		}
	}

	public void onSave(){
		Config.INSTANCE.save();
	}

	public void onStopping(MinecraftServer server){
		this.statuscode = InfoUploader.SERVER_STOPPING;
		if(this.uploader != null){
			this.uploader.sendServerStatus(InfoUploader.SERVER_STOPPING);
		}
		this.onSave();
	}

	public void onStopped(MinecraftServer server){
		this.statuscode = InfoUploader.SERVER_STOPPED;
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

	public void stopUploader(){
		if(this.uploader != null){
			this.uploader.sendServerStatus(InfoUploader.SERVER_UNKNOWN);
			this.uploader.close();
			this.uploader = null;
		}
	}

	public void startUploader() throws Exception{
		if(Config.INSTANCE.getEnable()){
			if(this.uploader != null){
				this.stopUploader();
			}
			try{
				final URI uploaduri = new URI(Config.INSTANCE.getUploadUrl());
				final String scheme = uploaduri.getScheme();
				if(scheme.equals("http") || scheme.equals("https")){
					this.uploader = new HttpInfoUploader(uploaduri);
				}else if(scheme.equals("ws") || scheme.equals("wss")){
					this.uploader = new WSInfoUploader(uploaduri);
				}else{
					throw new Exception("Unknown scheme: " + scheme);
				}
			}catch(URISyntaxException | MalformedURLException e){
				throw new Exception("Parse upload url error: " + e.getMessage());
			}
			if(this.uploader != null){
				this.uploader.sendServerStatus(this.statuscode);
			}
		}
	}

	public static Text createText(final String text){
		return new LiteralText(text);
	}
}
