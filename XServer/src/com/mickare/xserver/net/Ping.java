package com.mickare.xserver.net;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.mickare.xserver.XServerManager;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.util.CacheMap;
import com.mickare.xserver.util.Encryption;

public class Ping {
	
	
	private static CacheMap<String, Ping> pending = new CacheMap<String, Ping>(20);

	public static void addPendingPing(Ping ping) {
		synchronized(pending) {
			if(ping.started == -1) {
				pending.put(ping.key, ping);
			}
		}
	}
	
	public static void receive(String key, XServer server) {
		synchronized(pending) {
			Ping p = pending.get(key);
			if(p != null) {
				p.receive(server);
			}
		}
	}

	
	private final CommandSender sender;
	private final String key;
	
	private boolean resultprinted = false;
	
	private long started = -1;
	private final long timeout = 2000;
	private final HashMap<XServer, Long> responses = new HashMap<XServer, Long>();
	private final HashSet<XServer> waiting = new HashSet<XServer>();
	
	public Ping(CommandSender sender) {
		this(sender, "Ping");
	}
	
	public Ping(CommandSender sender, String salt) {
		this.sender = sender;
		this.key = Encryption.MD5(String.valueOf(Math.random()) + salt);
		
	}
	
	public boolean start() throws NotInitializedException {
		if(started == -1) {
			addPendingPing(this);
			synchronized(waiting) {
				for(XServer s : waiting.toArray(new XServer[waiting.size()])) {
					if(!s.isConnected()) {
						waiting.remove(s);
						responses.put(s, (long) -1);
					}
				}
			}
			started = System.currentTimeMillis();
			for(XServer s : waiting.toArray(new XServer[waiting.size()])) {
				try {
					s.ping(this);
				} catch (InterruptedException | IOException e) {
					waiting.remove(s);
					responses.put(s, Long.MAX_VALUE);
				}
			}
			check();
			XServerManager.getInstance().getThreadPool().runTask(new Runnable() {
				public void run() {
					try {
						Thread.sleep(timeout);
						check();
					} catch (InterruptedException e) {
					}
				}});
			return true;
		}
		return false;
	}
		
	public void add(XServer server) {
		responses.put(server, Long.MAX_VALUE);
		waiting.add(server);
	}
	
	public void addAll(Collection<XServer> servers) {
		for(XServer s : servers) {
			add(s);
		}
	}
	
	public void receive(XServer server) {
		long t = System.currentTimeMillis();
		synchronized(waiting) {
			if(waiting.contains(server)) {
				waiting.remove(server);
				synchronized(responses) {
					responses.put(server, t);
				}
				check();
			}
		}
	}
	
	private boolean check() {
		if(!isPending() && !resultprinted) {
			resultprinted = true;
			synchronized(waiting) {
				waiting.clear();
			}
			sender.sendMessage(getFormatedString());
			return true;
		}
		return false;
	}
	
	public boolean isPending() {
		synchronized(waiting) {
			return ((waiting.size() > 0) ? (System.currentTimeMillis() - started < timeout) : false); 
		}
	}
	
	public String getFormatedString() {
		synchronized(waiting) {
			if(waiting.size() > 0) {
				return "Still Pending...";
			} else {
				StringBuilder sb = new StringBuilder();
				for(Entry<XServer, Long> es : responses.entrySet()) {
					sb.append("\n").append(ChatColor.GOLD).append(es.getKey().getName()).append(ChatColor.GRAY).append(" - ");
					if(es.getValue() < 0) {
						sb.append(ChatColor.RED).append("Not connected!\n");
					} else if(es.getValue() == Long.MAX_VALUE) {
						if(es.getKey().isConnected()) {
							sb.append(ChatColor.RED).append("Timeout!\n");
						} else {
							sb.append(ChatColor.RED).append("Timeout! Connection lost!\n");
						}
					} else {
						long diff = es.getValue() - started;
						if(diff < 10) {
							sb.append(ChatColor.GREEN);
						} else if (diff < 30) {
							sb.append(ChatColor.YELLOW);
						} else if (diff < 100) {
							sb.append(ChatColor.GOLD);
						} else {
							sb.append(ChatColor.RED);
						}
						sb.append(diff).append("ms");
					}
				}
				return sb.toString();
			}
		}

	}

	public CommandSender getSender() {
		return sender;
	}

	public String getKey() {
		return key;
	}

	
}