package cc.bukkitPlugin.commons.local;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import cc.bukkitPlugin.commons.Log;
import cc.bukkitPlugin.commons.nmsutil.NMSUtil;
import cc.bukkitPlugin.commons.nmsutil.nbt.NBTUtil;
import cc.bukkitPlugin.commons.plugin.ABukkitPlugin;
import cc.bukkitPlugin.commons.plugin.INeedClose;
import cc.bukkitPlugin.commons.plugin.manager.AManager;
import cc.bukkitPlugin.commons.plugin.manager.fileManager.ILangModel;
import cc.commons.commentedyaml.CommentedYamlConfig;
import cc.commons.util.StringUtil;
import cc.commons.util.reflect.ClassUtil;
import cc.commons.util.reflect.FieldUtil;
import cc.commons.util.reflect.MethodUtil;

public class LocalLanguage<T extends ABukkitPlugin<T>>extends AManager<T> implements INeedClose,ILangModel{

    private static LocalLanguage<?> mInstance;
    // NMSItemStack
    public static final Method method_NMSItemStack_getName;
    public static final Method method_NMSItemStack_getUnlocalizedName;
    // Block
    public static final Method method_CraftWorld_getTileEntityAt;
    // NMSEnchant
    public static final Field field_CraftEnchantment_target;
    public static final Method method_NMSEnchantment_getUnlocalizedName;
    // Entity
    public static final Method method_NMSEntityInsentient_getCustomName;
    public static final Method method_NMSEntityInsentient_setCustomName;
    public static final Method method_NMSEntity_getName;
    public static final Method method_NMSEntity_writeToNBT;
    // LocalLang
    public static final Class<?> clazz_StringTranslate;
    public static final Object instance_StringTranslate;
    public static final Map<Object,Object> fieldValue_StringTranslate_Map;
    // FML
    protected final static boolean isFML;
    private static Map<String,Properties> modLanguageData;

    private static Properties en_USProp=null;
    public final static String en_US="en_US";
    private static String mNowLang="en_US";

    static{
        Class<?> tClazz;

        // NMS ItemStck
        ItemStack tItem=new ItemStack(Material.STONE);
        Object tNMSItem=NMSUtil.getNMSItem(tItem);
        // NMS ItemStack getName
        Object tItemTag=NBTUtil.newNBTTagCompound();
        Object tDisplayTag=NBTUtil.newNBTTagCompound();
        String displayName="1234567890",tUnlocalName_Stone="tile.stone";
        Object tNameTag=ClassUtil.newInstance(NBTUtil.clazz_NBTTagString,String.class,displayName);
        NBTUtil.invokeNBTTagCompound_set(tDisplayTag,"Name",tNameTag);
        NBTUtil.invokeNBTTagCompound_set(tItemTag,"display",tDisplayTag);
        NBTUtil.setItemNBT_NMS(tNMSItem,tItemTag);
        ArrayList<Method> tMayGetNameMethods=MethodUtil.getUnknowMethod(NMSUtil.clazz_NMSItemStack,String.class,true);
        int getNamePos=-1,getUnlocalNamePos=-1;
        for(int i=0;i<tMayGetNameMethods.size();i++){
            String getStr=(String)MethodUtil.invokeMethod(tMayGetNameMethods.get(i),tNMSItem);
            if(getStr.equals(displayName))
                getNamePos=i;
            if(getStr.startsWith(tUnlocalName_Stone))
                getUnlocalNamePos=i;
        }
        method_NMSItemStack_getName=tMayGetNameMethods.get(getNamePos);
        method_NMSItemStack_getUnlocalizedName=tMayGetNameMethods.get(getUnlocalNamePos);
        // NMS ItemStack getName-end
        // Lang
        isFML=ClassUtil.isClassLoaded("cpw.mods.fml.common.registry.LanguageRegistry");
        if(isFML){
            clazz_StringTranslate=ClassUtil.getClass("net.minecraft.util.StringTranslate");

            Class<?> cpwlang=ClassUtil.getClass("cpw.mods.fml.common.registry.LanguageRegistry");
            Object instance_LanguageRegistry=FieldUtil.getField(cpwlang,"instance",true);
            LocalLanguage.modLanguageData=(Map<String,Properties>)FieldUtil.getFieldValue(cpwlang,"modLanguageData",true,instance_LanguageRegistry);
        }else{
            clazz_StringTranslate=NMSUtil.getNMSClass("LocaleLanguage");
        }
        Field tField=FieldUtil.getField(clazz_StringTranslate,clazz_StringTranslate,-1,true).get(0);
        instance_StringTranslate=FieldUtil.getStaticFieldValue(tField);
        tField=FieldUtil.getField(clazz_StringTranslate,Map.class,-1,true).get(0);
        fieldValue_StringTranslate_Map=(Map<Object,Object>)FieldUtil.getFieldValue(tField,instance_StringTranslate);

        // enchant start
        tClazz=NMSUtil.getCBTClass("enchantments.CraftEnchantment");
        field_CraftEnchantment_target=FieldUtil.getField(tClazz,"target",true);
        tClazz=field_CraftEnchantment_target.getType();
        ArrayList<Method> tMethods=MethodUtil.getUnknowMethod(tClazz,String.class,true);
        int tPos=0;
        if(tMethods.size()>1){
            if(tMethods.get(tPos).getName().equals("toString"))
                tPos=1;
        }
        method_NMSEnchantment_getUnlocalizedName=tMethods.get(tPos);
        // enchant stop
        // block start
        Class<?> clazz_CraftWorld=NMSUtil.getCBTClass("CraftWorld");
        method_CraftWorld_getTileEntityAt=MethodUtil.getMethod(clazz_CraftWorld,"getTileEntityAt",new Class<?>[]{int.class,int.class,int.class},true);
        // block end
        // entity start
        Class<?> clazz_NMSWorld=MethodUtil.getMethod(clazz_CraftWorld,"getHandle",true).getReturnType().getSuperclass();
        Object tObjWorld=null;
        List<World> tWorlds=Bukkit.getWorlds();
        if(tWorlds!=null&&!tWorlds.isEmpty()){
            World tWorld=tWorlds.get(0);
            tObjWorld=MethodUtil.invokeMethod(tWorld.getClass(),"getHandle",true,tWorld);
        }
        tClazz=NMSUtil.getCBTClass("entity.CraftZombie");
        Class<?> clazz_NMSEntityZombie=MethodUtil.getMethod(tClazz,"getHandle",true).getReturnType();
        Object instance_NMSEntityZombie=ClassUtil.newInstance(clazz_NMSEntityZombie,clazz_NMSWorld,tObjWorld);

        String key="entity.Zombie.name";
        String testLang="1234\n5678";
        String oldLang=fieldValue_StringTranslate_Map.put(key,testLang).toString();
        tMethods=MethodUtil.getUnknowMethod(NMSUtil.clazz_NMSEntity,String.class,true);
        tPos=0;
        for(int i=0;i<tMethods.size();i++){ // 获取getName方法
            String tStr=(String)MethodUtil.invokeMethod(tMethods.get(i),instance_NMSEntityZombie);
            if(tStr.equals(testLang)){
                tPos=i;
                fieldValue_StringTranslate_Map.put(key,oldLang);
                break;
            }
        }
        method_NMSEntity_getName=tMethods.get(tPos);
        Class<?> clazz_NMSEntityInsentient=clazz_NMSEntityZombie.getSuperclass().getSuperclass().getSuperclass();
        if(MethodUtil.isMethodExist(clazz_NMSEntityInsentient,void.class,String.class,true)){
            //1.8及其以下
            method_NMSEntityInsentient_setCustomName=MethodUtil.getUnknowMethod(clazz_NMSEntityInsentient,void.class,String.class,true).get(0);
            tMethods=MethodUtil.getUnknowMethod(clazz_NMSEntityInsentient,String.class,true);
        }else{
            //1.9及其以上
            method_NMSEntityInsentient_setCustomName=MethodUtil.getUnknowMethod(NMSUtil.clazz_NMSEntity,void.class,String.class,true).get(0);
            tMethods=MethodUtil.getUnknowMethod(NMSUtil.clazz_NMSEntity,String.class,true);
        }
        tPos=0;
        MethodUtil.invokeMethod(method_NMSEntityInsentient_setCustomName,instance_NMSEntityZombie,"");
        for(Method sMethod : tMethods){
            String tValue=String.valueOf(MethodUtil.invokeMethod(sMethod,instance_NMSEntityZombie));
            if(tValue.isEmpty()){
                break;
            }
            tPos++;
        }
        method_NMSEntityInsentient_getCustomName=tMethods.get(tPos);
        tMethods=MethodUtil.getUnknowMethod(clazz_NMSEntityZombie,void.class,NBTUtil.clazz_NBTTagCompound,true);
        tPos=0;
        MethodUtil.invokeMethod(tMethods.get(0),instance_NMSEntityZombie,tItemTag);
        if(NBTUtil.getNBTTagCompoundValue(tItemTag).isEmpty()){
            tPos=1;
        }
        method_NMSEntity_writeToNBT=MethodUtil.getMethod(NMSUtil.clazz_NMSEntity,tMethods.get(tPos).getName(),NBTUtil.clazz_NBTTagCompound,true);
        // entity stop
    }

    protected LocalLanguage(T pPlugin){
        super(pPlugin);
        LocalLanguage.mInstance=this;
        this.mPlugin.registerCloseModel(this);
        this.mPlugin.getLangManager().registerLangModel(this);
    }

    public static <T extends ABukkitPlugin<T>> LocalLanguage<?> getInstance(T pPlugin){
        synchronized(LocalLanguage.class){
            if(LocalLanguage.mInstance==null){
                LocalLanguage.mInstance=new LocalLanguage<>(pPlugin);
                // 非本插件内实例化时,需要刷新默认语言翻译
                pPlugin.getLangManager().reloadModel(LocalLanguage.class);
            }
        }
        return LocalLanguage.mInstance;
    }

    @Override
    public void disable(){
        this.setLang(null,LocalLanguage.en_US);
    }

    synchronized protected void setLang(CommandSender pSender,String pLang){
        if(StringUtil.isEmpty(pLang))
            return;
        if(pLang.equals(LocalLanguage.mNowLang))
            return;
        Log.info(pSender,this.mPlugin.C("MsgRepaceOldLangToNewLang").replace("%lang_old%",LocalLanguage.mNowLang).replace("%lang_new%",pLang));
        LocalLanguage.mNowLang=pLang;

        // 备份en_US
        if(LocalLanguage.en_USProp==null){
            LocalLanguage.en_USProp=new Properties();
            LocalLanguage.en_USProp.putAll(LocalLanguage.fieldValue_StringTranslate_Map);
        }
        Map<String,String> tAllLang=new HashMap<>();
        if(isFML){ // 导入mod翻译
            Properties targetLang;
            if(pLang.equals(LocalLanguage.en_US)){
                targetLang=LocalLanguage.en_USProp;
            }else{
                targetLang=LocalLanguage.modLanguageData.get(pLang);
            }
            if(targetLang!=null){
                for(Map.Entry<Object,Object> sEntry : targetLang.entrySet()){
                    if(sEntry.getKey()==null||sEntry.getValue()==null)
                        continue;
                    tAllLang.put(sEntry.getKey().toString(),sEntry.getValue().toString());
                }
            }
        }
        Map<String,String> tExtraLang=this.getExtraLang();
        if(tExtraLang!=null&&!tExtraLang.isEmpty()){ // 导入翻译偏好
            for(Map.Entry<String,String> sEntry : this.getExtraLang().entrySet()){
                if(sEntry.getKey()==null)
                    continue;
                if(sEntry.getValue()==null)
                    continue;
                tAllLang.put(sEntry.getKey(),sEntry.getValue());
            }
        }
        if(!tAllLang.isEmpty()){ // 此处不进行clear以防止key缺失
            LocalLanguage.fieldValue_StringTranslate_Map.putAll(tAllLang);
        }
    }

    /**
     * 在把翻译导入FML中时调用,此处可以设置翻译偏好
     */
    protected Map<String,String> getExtraLang(){
        return new HashMap<>();
    }

    /**
     * 获取由物品ID格式化后的名字
     * <p>
     * 例如"MINECRAFT_DIRT"将会格式化未"Minecratf Dirt"
     * </p>
     */
    public static String getFormatId(ItemStack pItem){
        if(pItem==null)
            return "";
        char[] tID=pItem.getType().name().toLowerCase().replace("_"," ").toCharArray();
        tID[0]=(char)(tID[0]&(0xDF));
        for(int i=0;i<tID.length;i++){
            if(tID[i]==' '&&i<tID.length-1){
                if(tID[i+1]<'a'||tID[i+1]>'z')
                    continue;
                tID[i+1]=(char)(tID[i+1]&(0xDF));
                i++;
            }
        }
        return new String(tID);
    }

    /**
     * 获取指定材料和子id的翻译
     * 
     * @param pMate
     *            材料
     * @param pDamage
     *            子id
     * @return 翻译或者空字符串
     */
    public String getName(Material pMate,short pDamage){
        if(pMate==null)
            return "";
        if(pDamage<0)
            pDamage=0;
        return this.getName(new ItemStack(pMate,1,pDamage));
    }

    /**
     * 获取物品的本地化翻译
     * <p>
     * 如果翻译失败将返回空字符串或者翻译key
     * </p>
     * 
     * @param pItem
     *            物品
     */
    public String getName(ItemStack pItem){
        if(pItem==null)
            return "";
        // 去掉自定义的名字
        ItemStack tItem=pItem.clone();
        ItemMeta tMeta=tItem.getItemMeta();
        if(tMeta!=null&&tMeta.hasDisplayName()){
            tMeta.setDisplayName(null);
            tItem.setItemMeta(tMeta);
        }

        Object tNMSItem=NMSUtil.getNMSItem(tItem);
        if(tNMSItem==null)
            return LocalLanguage.getFormatId(tItem);
        return (String)MethodUtil.invokeMethod(method_NMSItemStack_getName,tNMSItem);
    }

    /**
     * 获取物品自定义名字,如果不存在则获取本地化翻译
     * <p>
     * 如果物品已经设定过名字,将使用已经设置过的名字 如果翻译失败将返回空字符串或者翻译key
     * </p>
     * 
     * @param pItem
     *            物品
     */
    public String getDisplayName(ItemStack pItem){
        if(pItem==null)
            return "";
        ItemMeta tMeta;
        if((tMeta=pItem.getItemMeta())!=null&&tMeta.hasDisplayName())
            return pItem.getItemMeta().getDisplayName();

        Object tNMSItem=NMSUtil.getNMSItem(pItem);
        if(tNMSItem==null)
            return LocalLanguage.getFormatId(pItem);
        return (String)MethodUtil.invokeMethod(method_NMSItemStack_getName,tNMSItem);
    }

    public String getName(Block pBlock){
        if(pBlock==null)
            return "";
        return this.getName(pBlock.getType(),pBlock.getData());
    }

    public String getDisplayName(Block pBlock){
        if(pBlock==null)
            return "";

        if(pBlock.getWorld()!=null&&MethodUtil.invokeMethod(method_CraftWorld_getTileEntityAt,pBlock.getWorld(),new Object[]{pBlock.getX(),pBlock.getY(),pBlock.getZ()})!=null){
            // TODO 需要根据TileEntity来写入物品NBT,然后生成对应的物品
        }
        return this.getName(pBlock.getType(),pBlock.getData());
    }

    /**
     * 获取附魔的本地化翻译
     * <p>
     * 如果翻译不存在,首先返回{@link Enchantment#getName()},如果此方法 返回的字符串包含特殊字符,将只返回附魔的id
     * </p>
     * 
     * @param pEnchant
     *            附魔
     */
    public String getDisplayName(Enchantment pEnchant){
        String unlocalName=this.getUnlocalizedName(pEnchant);
        if(StringUtil.isEmpty(unlocalName)||StringUtil.isEmpty((unlocalName=this.getStringLocalization(unlocalName)))){
            return pEnchant.getName().matches("[a-zA-Z0-9_]+")?pEnchant.getName():pEnchant.getId()+"";
        }else return unlocalName;
    }

    /**
     * 获取附魔的本地化翻译
     * <p>
     * 如果翻译不存在,首先返回{@link Enchantment#getName()},如果此方法 返回的字符串包含特殊字符,将只返回附魔的id
     * </p>
     * 
     * @param pEnchant
     *            附魔
     */
    public String getDisplayName(Enchantment pEnchant,int pLevel){
        String tName=this.getDisplayName(pEnchant);
        if(StringUtil.isEmpty(tName)||pLevel<=0)
            return tName;

        return tName+" "+this.intToString(pLevel);

    }

    public String intToString(int pLevel){
        String unlocalLevel="enchantment.level."+pLevel;
        String localLevel=this.getStringLocalization(unlocalLevel);
        if(unlocalLevel.equals(localLevel)){
            return pLevel+"";
        }else{
            return localLevel;
        }
    }

    /**
     * 获取实体的名称
     * 
     * @param pEntity
     *            实体
     * @return 名称或者空字符串
     */
    public String getName(Entity pEntity){
        if(pEntity==null)
            return "";

        if(pEntity instanceof Player){
            String tLang=this.mPlugin.C("WordPlayer");
            if(StringUtil.isEmpty(tLang)||tLang.equals("WordPlayer"))
                return "Player";
            else return tLang;
        }
        Object tNMSEntity=NMSUtil.getNMSEntity(pEntity);
        if(tNMSEntity==null)
            return pEntity.getClass().getSimpleName().replace("Craft","").replace("Entity","");
        if(method_NMSEntityInsentient_getCustomName.getDeclaringClass().isInstance(tNMSEntity)){
            String customeName=(String)MethodUtil.invokeMethod(method_NMSEntityInsentient_getCustomName,tNMSEntity);
            if(StringUtil.isNotEmpty(customeName)){
                MethodUtil.invokeMethod(method_NMSEntityInsentient_setCustomName,tNMSEntity,"");
                String tName=(String)MethodUtil.invokeMethod(method_NMSEntity_getName,tNMSEntity);
                MethodUtil.invokeMethod(method_NMSEntityInsentient_setCustomName,tNMSEntity,customeName);
                return tName;
            }
        }
        return (String)MethodUtil.invokeMethod(method_NMSEntity_getName,tNMSEntity);
    }

    /**
     * 获取实体自定的名称
     * 
     * @param pEntity
     *            实体
     * @return 自定义名称或名称或空字符串
     */
    public String getDisplayName(Entity pEntity){
        if(pEntity==null)
            return "";

        if(pEntity instanceof Player){
            return ((Player)pEntity).getName();
        }

        Object tNMSEntity=NMSUtil.getNMSEntity(pEntity);
        if(tNMSEntity==null)
            return pEntity.getClass().getSimpleName().replace("Craft","").replace("Entity","");

        return (String)MethodUtil.invokeMethod(method_NMSEntity_getName,tNMSEntity);
    }

    /**
     * 获取物品未本地化时的语言key
     * <p>
     * 如果获取失败将返回空字符串
     * </p>
     * 
     * @param pItem
     *            物品
     * @return 物品语言key或者空字符串
     */
    public String getUnlocalizedName(ItemStack pItem){
        if(pItem==null)
            return "";
        Object tNMSItem=NMSUtil.getNMSItem(pItem);
        if(tNMSItem==null)
            return "";
        return (String)MethodUtil.invokeMethod(method_NMSItemStack_getUnlocalizedName,tNMSItem);
    }

    /**
     * 获取附魔的未本地化时的语言key
     * <p>
     * 如果获取失败将返回空字符串
     * </p>
     * 
     * @param pEnchant
     *            附魔
     * @return 附魔语言key或者空字符串
     */
    public String getUnlocalizedName(Enchantment pEnchant){
        if(pEnchant==null)
            return "";

        Object tNMSEnchant=FieldUtil.getFieldValue(field_CraftEnchantment_target,pEnchant);
        if(tNMSEnchant==null)
            return "";
        return String.valueOf(MethodUtil.invokeMethod(method_NMSEnchantment_getUnlocalizedName,tNMSEnchant));
    }

    /**
     * 获取未本地化的key对应的本地化的翻译
     * 
     * @param pKey
     *            key
     * @return 本地化翻译或者null
     */
    public String getStringLocalization(String pKey){
        Object tValue=LocalLanguage.fieldValue_StringTranslate_Map.get(pKey);
        if(tValue==null)
            return null;
        else return tValue.toString();
    }

    @Override
    public void addDefaultLang(CommentedYamlConfig pConfig){
        pConfig.addDefault("MsgRepaceOldLangToNewLang","替换语言%lang_old%为%lang_new%");
        pConfig.addDefault("MsgErrorHappendWhenImportLang","导入语言时发生了错误");
        pConfig.addDefault("WordPlayer","玩家");
    }

    @Override
    public void setLang(CommentedYamlConfig pConfig){}

}
