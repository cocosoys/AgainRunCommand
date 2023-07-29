package soys.againruncommand;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AgainRunCommand extends JavaPlugin implements Listener {
    public static AgainRunCommand instance;
    public static YamlConfiguration config;
    public static int debugMaxCount=10;
    public static int cycleDebugCount=99;
    public static boolean debugMode=false;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this,this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender.isOp()){
            if(args.length==1){
                if(args[0].equals("help")){
                    sender.sendMessage("/arc reload 重载配置文件");
                    sender.sendMessage("/arc help 帮助列表");
                    sender.sendMessage("/arc debug 开启debug模式,可观察执行情况,再次输入该指令则关闭");
                }
                if(args[0].equals("reload")){
                    loadConfig();
                    sender.sendMessage("[arc]成功重载配置文件");
                }
                if(args[0].equals("debug")){
                    debugMode=!debugMode;
                    sender.sendMessage("[arc]开启debug模式,后台将输出执行情况,再次输入/arc debug关闭");
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if(sender.isOp()){
            return Arrays.asList("debug","reload","help");
        }
        return null;
    }

    @EventHandler
    public void commandPlayerListener(PlayerCommandPreprocessEvent e){
        commandListener(new ServerCommandEvent(e.getPlayer(), e.getMessage()));
    }

    @EventHandler
    public void commandListener(ServerCommandEvent e){
        // 去掉指令的 / 前缀
        String serverCommand=e.getCommand();
        serverCommand=serverCommand.replace("/","");

        // 搜索所有匹配的指令检测名
        MatchingRule matchingRule=new MatchingRule(serverCommand,config.getKeys(false).toArray(new String[0]));
        matchingRule.init();

        // 分割指令参数
        String[] allArgs=serverCommand.split(" ");
        // 获取指令名称
        String commandName=allArgs[0];

        // 运行次数,用于是否多次触发和debug
        int runCount=0;

        // 执行了多少次相同的指令检测,用于debug
        int debugCount=0;
        String lastCommandRuleName="";

        for (String commandRuleName : matchingRule.getRuleStringList()) {
            if(debugCount>debugMaxCount){
                warningInfo("运行状态:检测到反复运行相同的指令检测,已终止所有运行.指令检测名:"+commandRuleName);
                break;
            }

            // 获取指令检测的配置参数
            ConfigurationSection command=config.getConfigurationSection(commandRuleName);

            boolean enable=command.getBoolean("enable",false);
            // 是否运行
            if(!enable){
                debugInfo("运行状态:未启动该指令检测,跳过该指令检测.指令检测名:"+commandRuleName);
                continue;
            }

            boolean run=command.getBoolean("run",false);
            // 是否执行指令
            if(run) {
                debugInfo("运行状态:禁止指令<" + serverCommand + ">执行.指令检测名:" + commandRuleName);
                e.setCancelled(true);
            }

            boolean op=command.getBoolean("op",true);
            // 是否仅op触发
            if(op){
                if(!e.getSender().isOp()){
                    debugInfo("运行状态:触发者<"+e.getSender().getName()+">没有OP权限,跳过该指令检测.指令检测名:"+commandRuleName);
                    continue;
                }
            }

            boolean with=command.getBoolean("with",true);
            // 是否多次触发
            if(!with){
                if(runCount!=0){
                    debugInfo("运行状态:已禁止多次执行,跳过该指令检测.指令检测名:"+commandRuleName);
                    continue;
                }
            }


            // 寻找玩家
            Player senderPlayer=null;
            if(e.getSender() instanceof Player){
                senderPlayer= (Player) e.getSender();
            }

            List<String> commandList=command.getStringList("commands");
            for (String cmd : commandList) {
                // 替换字符串
                cmd=replaceAll_MyFunc(cmd,commandName,allArgs);
                if(cmd.startsWith("[player]")){
                    // 玩家自己执行
                    cmd=cmd.substring("[player]".length());
                    cmd=setPlaceholders(senderPlayer,cmd);
                    if(senderPlayer==null){
                        runCommandConsole(cmd);
                    }else{
                        runCommandPlayer(senderPlayer,cmd);
                    }
                }else if(cmd.startsWith("[op]")){
                    // 以op权限执行
                    cmd=cmd.substring("[op]".length());
                    cmd=setPlaceholders(senderPlayer,cmd);
                    if(senderPlayer==null){
                        runCommandConsole(cmd);
                    }else {
                        runCommandOP(senderPlayer, cmd);
                    }
                }else if(cmd.startsWith("[console]")){
                    // 后台执行
                    cmd=cmd.substring("[console]".length());
                    cmd=setPlaceholders(senderPlayer,cmd);
                    runCommandConsole(cmd);
                }else{
                    // 玩家自己执行
                    cmd=setPlaceholders(senderPlayer,cmd);
                    if(senderPlayer==null){
                        runCommandConsole(cmd);
                    }else{
                        runCommandPlayer(senderPlayer,cmd);
                    }
                }
            }

            // debug
            if(runCount>cycleDebugCount){
                warningInfo("运行状态:检测到反复执行超过99次,怀疑为异常情况,请打开debug功能查看运行状态!当前执行的指令检测名:" + commandRuleName);
                break;
            }

            //增加执行次数
            runCount++;

            //检测是否执行相同的指令检测
            if(lastCommandRuleName.equals(commandRuleName)){
                debugCount++;
            }else{
                debugCount=0;
            }
            lastCommandRuleName=commandRuleName;
        }
        /* 早期仅匹配指令名称的写法
        String[] allArgs=serverCommand.split(" ");
        String commandName=allArgs[0].replace("/","");
        if(config.isConfigurationSection(commandName)){
            ConfigurationSection command=config.getConfigurationSection(commandName);

            boolean enable=command.getBoolean("enable",false);
            // 是否运行
            //getLogger().info("已开启检测:"+commandName);
            if(!enable){
                debugInfo("运行状态:未启动该指令检测.指令检测名:");
                return;
            }

            boolean run=command.getBoolean("run",false);
            // 是否执行指令
            //getLogger().info("指令终止情况:"+run);
            debugInfo("运行状态:禁止指令<"+serverCommand+">执行.指令检测名:");
            e.setCancelled(run);

            boolean op=command.getBoolean("op",true);
            // 是否仅op触发
            //getLogger().info("检测是否op触发:"+op);
            if(op){
                if(!e.getSender().isOp()){
                    debugInfo("运行状态:触发者<"+e.getSender().getName()+">没有OP权限.指令检测名:");
                    //getLogger().info("非op被禁止触发:"+e.getSender().getName());
                    return;
                }
            }

            // 寻找玩家
            Player senderPlayer=null;
            //getLogger().info("寻找玩家");
            if(e.getSender() instanceof Player){
                senderPlayer= (Player) e.getSender();
                //getLogger().info("找到玩家:"+ senderPlayer.getName());
            }

            List<String> commandList=command.getStringList("commands");
            for (String cmd : commandList) {
                cmd=replaceAll_MyFunc(cmd,commandName,allArgs);
                if(cmd.startsWith("[player]")){
                    // 玩家自己执行
                    //getLogger().info("状态:玩家自己执行");

                    cmd=cmd.substring("[player]".length());
                    cmd=setPlaceholders(senderPlayer,cmd);
                    if(senderPlayer==null){
                        runCommandConsole(cmd);
                    }else{
                        runCommandPlayer(senderPlayer,cmd);
                    }
                }else if(cmd.startsWith("[op]")){
                    // 以op权限执行
                    //getLogger().info("状态:以OP权限执行");

                    cmd=cmd.substring("[op]".length());
                    cmd=setPlaceholders(senderPlayer,cmd);
                    if(senderPlayer==null){
                        runCommandConsole(cmd);
                    }else {
                        runCommandOP(senderPlayer, cmd);
                    }
                }else if(cmd.startsWith("[console]")){
                    // 后台执行
                    //getLogger().info("状态:后台执行");

                    cmd=cmd.substring("[console]".length());
                    cmd=setPlaceholders(senderPlayer,cmd);
                    runCommandConsole(cmd);
                }else{
                    // 玩家自己执行
                    //getLogger().info("状态:玩家自己执行");

                    cmd=setPlaceholders(senderPlayer,cmd);
                    if(senderPlayer==null){
                        runCommandConsole(cmd);
                    }else{
                        runCommandPlayer(senderPlayer,cmd);
                    }
                }
            }
        }
        */
    }

    public static String replaceAll_MyFunc(String str,String cmd,String[] args){
        str=str.replaceAll("&","§");
        str=str.replaceAll("<commandName>",cmd);
        for (int i = 0; i < args.length; i++) {
            str=str.replaceAll("<args"+i+">",args[i]);
        }
        return str;
    }

    public static void runCommandPlayer(Player player,String command){
        getInstance().getServer().dispatchCommand(player, command);
    }

    public static void runCommandOP(Player player,String command){
        boolean op=player.isOp();
        player.setOp(true);
        try {
            runCommandPlayer(player,command);
        } finally {
            player.setOp(op);
        }
    }

    public static void runCommandConsole(String command){
        ConsoleCommandSender consoleSender = getInstance().getServer().getConsoleSender();
        getInstance().getServer().dispatchCommand(consoleSender, command);
    }

    public static String setPlaceholders(Player player, String str){
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI")!=null){
            str=me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player,str);
        }
        return str;
    }

    public static AgainRunCommand getInstance(){
        return instance;
    }

    public static void loadConfig(){
        getInstance().saveDefaultConfig();
        config = YamlConfiguration.loadConfiguration(new File(AgainRunCommand.getInstance().getDataFolder(),"config.yml"));
    }

    public static void debugInfo(String mess){
        if(debugMode) {
            Bukkit.getLogger().info("[arc][Debug模式] -> "+mess);
        }
    }

    public static void warningInfo(String mess){
        Bukkit.getLogger().warning("[arc] -> "+mess);
    }
}
