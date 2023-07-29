package soys.againruncommand;

import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.HashMap;

public class MatchingRule {
    public String string;
    public String[] ruleStringList;

    public MatchingRule(String string,String[] ruleStringList){
        this.string=string;
        this.ruleStringList=ruleStringList;
    }

    // 允许一切奇思妙想写法
    public void init(){
        // 匹配的列表
        ArrayList<String> newRuleString=new ArrayList<>();

        // 分割指令各个参数
        String[] stringList=this.string.split(" ");

        // 循环匹配
        for (String rule : this.ruleStringList) {
            String[] ruleStringList=rule.split(" ");

            // 特殊情况:当指令检测长度为1时
            if(ruleStringList.length==1){
                String ruleCommandName=ruleStringList[0];
                // 若指令不为 <null>
                if(!ruleCommandName.equals("<null>")){
                    continue;
                }
                // 若指令不为 [xxx]
                if(!selectMatching(ruleCommandName,stringList[0])){
                    continue;
                }
                // 若指令不匹配
                if(!ruleCommandName.equals(stringList[0])){
                    AgainRunCommand.debugInfo("匹配状态:当前指令长度为1.指令检测名:"+rule+",指令:"+stringList[0]+",两者不匹配,跳过该指令检测");
                    continue;
                }
            }

            // 允许 '<null> xxx' 和 '[xxx] xxx' 写法
            if(ruleStringList.length>=2){
                // 'xxx指令 xxx xxx' 其中的最后一个参数是否为 [xxx] 格式,此处ruleStringList.length只能大于等于3才符合条件
                if(ruleStringList.length-1==stringList.length){
                    String endArgs=ruleStringList[ruleStringList.length-1];
                    if(!(endArgs.startsWith("[") && endArgs.startsWith("]"))){
                        AgainRunCommand.debugInfo("匹配状态:长度与指令不匹配,跳过该指令检测.指令检测名:"+rule);
                        continue;
                    }
                }

                // false为匹配到指令检测,true为匹配不到指令检测
                boolean matching=false;

                for (int i = 0; i < stringList.length; i++) {
                    String ruleString=ruleStringList[i];
                    String matchingString=stringList[i];
                    if(ruleString.equals("<null>")){
                        continue;
                    }
                    if(selectMatching(ruleString,matchingString)){
                        continue;
                    }
                    /*
                    if(ruleString.startsWith("[") && ruleString.endsWith("]")){
                        String selectTarget=ruleString.substring(1,ruleString.length()-1);
                        if(selectTarget.startsWith("*")){
                            String target=selectTarget.substring(1);
                            // 是否匹配到正确的参数
                            boolean type=false;
                            if(target.equals("player")){
                                // 玩家名称匹配规则
                                for (Player onlinePlayer : AgainRunCommand.getInstance().getServer().getOnlinePlayers()) {
                                    if(onlinePlayer.getName().equals(matchingString)){
                                        type=true;
                                        break;
                                    }
                                }
                            }else if(target.equals("world")){
                                // 世界名称匹配规则
                                for (World world : AgainRunCommand.getInstance().getServer().getWorlds()) {
                                    if(world.getName().equals(matchingString)){
                                        type=true;
                                        break;
                                    }
                                }
                            }else if(target.equals("string")){
                                // 字符串类型匹配规则
                                try{
                                    Double.parseDouble(matchingString);
                                }catch (NumberFormatException var3) {
                                    // 符合运行规则
                                    type=true;
                                }
                            }else if(target.equals("number")){
                                // 数字类型匹配规则
                                try{
                                    Double.parseDouble(matchingString);
                                    // 符合运行规则
                                    type=true;
                                }catch (NumberFormatException ignored) {}
                            }
                            // 符合参数匹配规则
                            if(type){
                                continue;
                            }
                        }
                        // 符合选择性匹配规则
                        if(matchingString.equals(selectTarget)){
                            continue;
                        }
                    }
                    */
                    // 符合精准匹配规则
                    if(ruleString.equals(matchingString)){
                        continue;
                    }
                    // 不符合任意规则,下文将跳过该指令检测匹配
                    matching=true;
                    break;
                }

                // 不匹配的指令检测,不将当前指令检测列入匹配范围
                if(matching){
                    AgainRunCommand.debugInfo("匹配状态:匹配不到指令检测.指令检测名:"+rule);
                    continue;
                }
            }
            // 将指令检测列入匹配范围
            AgainRunCommand.debugInfo("匹配状态:---成功检测到指令,已加入执行列表.指令检测名:"+rule);

            newRuleString.add(rule);
        }

        // 是否开启debug模式输出内容
        if(AgainRunCommand.debugMode) {
            for (int i = 0; i < 3; i++) {
                AgainRunCommand.debugInfo("匹配状态:已完成所有匹配,符合的项目将开始执行...");
            }
        }

        // 得到新的指令检测范围
        this.ruleStringList= newRuleString.toArray(new String[0]);
    }

    /*禁止各种奇思妙想的写法
    public void init(){
        // 匹配的列表
        ArrayList<String> newRuleString=new ArrayList<>();
        HashMap<String,Integer> newRuleStringMap=new HashMap<>();

        // 分割指令各个参数
        String[] stringList=this.string.split(" ");

        // 循环匹配
        for (String rule : this.ruleStringList) {
            String[] ruleStringList=rule.split(" ");

            // 特殊情况:禁止只有<null>或者只有[xxx]或者只有<null>和[xxx]的写法
            int specialString=0;
            int selectString=0;
            for (String string : ruleStringList) {
                if(string.equals("<null>")){
                    specialString++;
                }
                if(string.startsWith("[") && string.endsWith("]")){
                    selectString++;
                }
            }
            // 不符合写法,结束本次循环
            if(specialString+selectString==ruleStringList.length){
                AgainRunCommand.debugInfo("匹配状态:禁止只有<null>或者只有[xxx]或者只有<null>和[xxx]的写法.指令检测名:"+rule);
                continue;
            }

            // 特殊情况:当指令检测长度为1时,禁止部分写法,如<null>,[xxx]
            if(stringList.length==1 && ruleStringList.length==1){
                String ruleCommandName=ruleStringList[0];
                // 若指令为 <null>
                if(ruleCommandName.equals("<null>")){
                    continue;
                }
                // 若指令为 [xxx]
                if(ruleCommandName.startsWith("[") && ruleCommandName.endsWith("]")){
                    continue;
                }
                // 若指令不匹配
                if(!ruleCommandName.equals(stringList[0])){
                    AgainRunCommand.debugInfo("匹配状态:当前指令长度为1.指令检测名:"+rule+",指令:"+stringList[0]+",两者不匹配,跳过该指令检测");
                    continue;
                }
                this.ruleStringList=new String[]{rule};
                return;
            }

            // 检测长度大于等于2时的写法,允许 <null> xxx 和 [xxx] xxx 写法
            if(stringList.length>=2 && ruleStringList.length==stringList.length || ruleStringList.length-1==stringList.length){
                // xxx指令 xxx xxx 其中的最后一个参数是否为 [xxx] 格式,禁止除了 [xxx] 以外的写法
                if(ruleStringList.length-1==stringList.length){
                    String endArgs=ruleStringList[ruleStringList.length-1];
                    if(!(endArgs.startsWith("[") && endArgs.startsWith("]"))){
                        AgainRunCommand.debugInfo("匹配状态:长度与指令不匹配,跳过该指令检测.指令检测名:"+rule);
                        continue;
                    }
                }

                // false为匹配到指令检测,true为匹配不到指令检测
                boolean matching=false;

                for (int i = 0; i < stringList.length; i++) {
                    String ruleString=ruleStringList[i];
                    String matchingString=stringList[i];
                    if(ruleString.equals("<null>")){
                        continue;
                    }
                    if(ruleString.startsWith("[") && ruleString.endsWith("]")){
                        String selectTarget=ruleString.substring(1,ruleString.length()-1);
                        if(selectTarget.startsWith("*")){
                            String target=selectTarget.substring(1,ruleString.length());
                            // 是否匹配到正确的参数
                            boolean type=false;
                            if(target.equals("player")){
                                // 玩家名称匹配规则
                                for (Player onlinePlayer : AgainRunCommand.getInstance().getServer().getOnlinePlayers()) {
                                    if(onlinePlayer.getName().equals(matchingString)){
                                        type=true;
                                        break;
                                    }
                                }
                            }else if(target.equals("world")){
                                // 世界名称匹配规则
                                for (World world : AgainRunCommand.getInstance().getServer().getWorlds()) {
                                    if(world.getName().equals(matchingString)){
                                        type=true;
                                        break;
                                    }
                                }
                            }else if(target.equals("string")){
                                // 字符串类型匹配规则
                                try{
                                    Double.parseDouble(matchingString);
                                }catch (NumberFormatException var3) {
                                    // 符合运行规则
                                    type=true;
                                }
                            }else if(target.equals("number")){
                                // 数字类型匹配规则
                                try{
                                    Double.parseDouble(matchingString);
                                    // 符合运行规则
                                    type=true;
                                }catch (NumberFormatException ignored) {}
                            }
                            // 符合参数匹配规则
                            if(type){
                                continue;
                            }
                        }
                        // 符合选择性匹配规则
                        if(matchingString.equals(selectTarget)){
                            continue;
                        }
                    }
                    // 符合精准匹配规则
                    if(ruleString.equals(matchingString)){
                        continue;
                    }
                    // 不符合任意规则,下文将跳过该指令检测匹配
                    matching=true;
                    break;
                }

                // 不匹配的指令检测,不将当前指令检测列入匹配范围
                if(matching){
                    AgainRunCommand.debugInfo("匹配状态:匹配不到指令检测.指令检测名:"+rule);
                    continue;
                }
            }
            // 将指令检测列入匹配范围
            AgainRunCommand.debugInfo("匹配状态:---成功检测到指令,已加入执行列表.指令检测名:"+rule);

            // 优先级算法
            int specialNumber1=500;
            int specialNumber2=25;
            if(specialString==0){

            }
            int priority=specialString*specialNumber2+selectString*specialNumber1;


            newRuleStringMap.put(rule,priority);
            newRuleString.add(rule);
        }
        if(AgainRunCommand.debugMode) {
            for (int i = 0; i < 3; i++) {
                AgainRunCommand.debugInfo("匹配状态:已完成所有匹配,符合的项目将开始执行...");
            }
        }



        // 得到新的指令检测范围
        this.ruleStringList= newRuleString.toArray(new String[0]);
    }
    */

    public boolean selectMatching(String ruleString,String matchingString){
        if(ruleString.startsWith("[") && ruleString.endsWith("]")){
            String selectTarget=ruleString.substring(1,ruleString.length()-1);
            if(selectTarget.startsWith("*")){
                String target=selectTarget.substring(1);
                // 是否匹配到正确的参数
                boolean type=false;
                if(target.equals("player")){
                    // 玩家名称匹配规则
                    for (Player onlinePlayer : AgainRunCommand.getInstance().getServer().getOnlinePlayers()) {
                        if(onlinePlayer.getName().equals(matchingString)){
                            type=true;
                            break;
                        }
                    }
                }else if(target.equals("world")){
                    // 世界名称匹配规则
                    for (World world : AgainRunCommand.getInstance().getServer().getWorlds()) {
                        if(world.getName().equals(matchingString)){
                            type=true;
                            break;
                        }
                    }
                }else if(target.equals("string")){
                    // 字符串类型匹配规则
                    try{
                        Double.parseDouble(matchingString);
                    }catch (NumberFormatException var3) {
                        // 符合运行规则
                        type=true;
                    }
                }else if(target.equals("number")){
                    // 数字类型匹配规则
                    try{
                        Double.parseDouble(matchingString);
                        // 符合运行规则
                        type=true;
                    }catch (NumberFormatException ignored) {}
                }
                // 符合参数匹配规则
                return type;
            }
            // 符合选择性匹配规则
            if(matchingString.equals(selectTarget)){
                return true;
            }
        }
        return false;
    }

    public String getString(){
        return string;
    }

    public String[] getRuleStringList(){
        return ruleStringList;
    }
}
