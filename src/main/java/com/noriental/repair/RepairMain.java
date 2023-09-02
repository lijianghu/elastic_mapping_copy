package com.noriental.repair;

import com.noriental.repair.handler.EsMetaExportHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/***
 * *   ____  ___________  ___________           ________   ____  __.  _____ _____.___.
 * *   \   \/  /\______ \ \_   _____/           \_____  \ |    |/ _| /  _  \\__  |   |
 * *    \     /  |    |  \ |    __)     ______   /   |   \|      <  /  /_\  \/   |   |
 * *    /     \  |    `   \|     \     /_____/  /    |    \    |  \/    |    \____   |
 * *   /___/\  \/_______  /\___  /              \_______  /____|__ \____|__  / ______|
 * *    	 \_/        \/     \/                       \/        \/       \/\/
 * *
 * 程序入口
 * 由于update_time为datetime类型，几亿大的表难以按范围查询。 因此设计思路如下：
 * 1. 通过订阅记录id, studentid, publishid, updatetime，按天/月存储。
 * 2. 通过工具类扫描文件的形式replace into修复数据。
 * 3:支持根据ES查询，修复数据
 * java -jar CanalDbSyncTools.jar
 *
 *
 */
public class RepairMain {
    public static void main(String[] args) throws Exception {
        EsMetaExportHandle.execute();
    }
}
