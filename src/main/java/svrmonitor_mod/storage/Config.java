
package com.github.kpnmserver.svrmonitor_mod.storage;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import com.github.kpnmserver.svrmonitor_mod.SvrMonitorMod;
import com.github.kpnmserver.svrmonitor_mod.util.JsonUtil;

public final class Config{
	public static final Config INSTANCE = new Config();
	private Config(){}

	static final class Item {
		@SerializedName("enable")
		public boolean enable = false;
		@SerializedName("upload_time")
		public int uploadtime = 20;
		@SerializedName("upload_url")
		public String uploadurl = "ws://url.to/upload";
		@SerializedName("upload_token")
		public String uploadtoken = "";

		public Item(){}
	}

	private Item item = new Item();

	public void setEnable(boolean enable){
		this.item.enable = enable;
	}

	public boolean getEnable(){
		return this.item.enable;
	}

	public int getUploadTime(){
		return this.item.uploadtime;
	}

	public String getUploadUrl(){
		return this.item.uploadurl;
	}

	public String getUploadToken(){
		return this.item.uploadtoken;
	}

	public void setUploadToken(final String tk){
		this.item.uploadtoken = tk;
	}

	public void reload(){
		final File file = new File(SvrMonitorMod.INSTANCE.getDataFolder(), "config.json");
		if(!file.exists()){
			return;
		}
		try(
			FileReader freader = new FileReader(file);
			JsonReader jreader = new JsonReader(freader);
		){
			this.item = JsonUtil.GSON.fromJson(jreader, Item.class);
		}catch(IOException e){
			SvrMonitorMod.LOGGER.error("load config file error:\n", e);
		}
	}

	public void save(){
		final File file = new File(SvrMonitorMod.INSTANCE.getDataFolder(), "config.json");
		if(!file.exists()){
			try{
				file.createNewFile();
			}catch(IOException e){
				SvrMonitorMod.LOGGER.error("create config file error:\n", e);
				return;
			}
		}
		try(
			FileWriter fwriter = new FileWriter(file);
			JsonWriter jwriter = new JsonWriter(fwriter);
		){
			jwriter.setIndent("  ");
			JsonUtil.GSON.toJson(this.item, Item.class, jwriter);
		}catch(IOException e){
			SvrMonitorMod.LOGGER.error("save config file error:\n", e);
		}
	}
}