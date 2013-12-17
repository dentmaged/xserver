package com.mickare.xserver;

import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.plugin.java.JavaPlugin;

public class BukkitEventHandler extends EventHandlerObj<JavaPlugin>
{

	// Spray those tasks over some ticks...
	private static final int SYNCTASK_SPRAY = 50;

	private final JavaPlugin plugin;

	private AtomicInteger syncTasksCount = new AtomicInteger(0);

	protected BukkitEventHandler(JavaPlugin plugin)
	{
		super(plugin.getLogger());
		this.plugin = plugin;
		plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
			@Override
			public void run()
			{
				int val = syncTasksCount.get();
				if (syncTasksCount.get() > SYNCTASK_SPRAY)
				{
					syncTasksCount.set(val - SYNCTASK_SPRAY);
				} else
				{
					syncTasksCount.set(0);
				}
			}
		}, SYNCTASK_SPRAY, 1);
	}

	@Override
	public void runTask(Boolean sync, XServerListenerPlugin<JavaPlugin> plugin, Runnable run)
	{
		boolean s = true;
		if (sync != null)
		{
			s = sync.booleanValue();
		}
		if (plugin.getPlugin().isEnabled())
		{

			if (s)
			{
				int st = syncTasksCount.incrementAndGet();
				if (st < SYNCTASK_SPRAY)
				{
					plugin.getPlugin().getServer().getScheduler().runTask(plugin.getPlugin(), run);
				} else
				{
					plugin.getPlugin().getServer().getScheduler().runTaskLater(plugin.getPlugin(), run, st / SYNCTASK_SPRAY);
				}
			} else
			{
				//plugin.getPlugin().getServer().getScheduler().runTaskAsynchronously(plugin.getPlugin(), run);
				run.run();
			}
		}
	}

	public JavaPlugin getPlugin()
	{
		return plugin;
	}

	@Override
	public void registerListener(JavaPlugin plugin, XServerListener lis)
	{
		XServerListenerPlugin<JavaPlugin> lp = null;
		lp = this.getListPlugin(plugin);
		if (lp == null)
		{
			lp = new BukkitListenerPlugin(plugin);
		}
		this.registerListener(lp, lis);
	}

}
