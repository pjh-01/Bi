package com.pjh.Bi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pjh.Bi.mapper.ChartMapper;
import com.pjh.Bi.model.entity.Chart;
import com.pjh.Bi.service.ChartService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author 宇宙无敌超级大帅哥
 * @description 针对表【chart(图表信息表)】的数据库操作Service实现
 * @createDate 2024-01-27 14:46:06
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public boolean createTableForCsvData(String csvData, String id) {
        try {
            String[] lines = csvData.split("\n");
            String[] headers = lines[0].split(",");
            StringBuilder createTableSQL = new StringBuilder();
            //1.建表语句
            createTableSQL.append("create table chart_").append(id).append("\n");
            createTableSQL.append("(");
            for (int i = 0; i < headers.length - 1; ++i) {
                createTableSQL.append(headers[i]).append(" text").append(" null,").append("\n");
            }
            createTableSQL.append(headers[headers.length - 1]).append(" text").append(" null").append("\n");
            createTableSQL.append(");").append("\n");
            //2.初始化插入数据
            StringBuilder initDataSQL = new StringBuilder();

            for (int i = 1; i < lines.length; ++i) {
                String[] elements = lines[i].split(",");
                initDataSQL.append("INSERT INTO chart_").append(id).append(" VALUES (");
                for(int j=0;j< elements.length-1;++j){
                    initDataSQL.append("'").append(elements[j]).append("', ");
                }
                initDataSQL.append("'").append(elements[elements.length - 1]).append("');").append("\n");
            }
            String[] insertSQLList = initDataSQL.toString().split("\n");
            //3.执行动态sql，建表
            jdbcTemplate.execute(createTableSQL.toString());
            //4.插入原始数据，注意接收的是数组
            jdbcTemplate.batchUpdate(insertSQLList);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}




