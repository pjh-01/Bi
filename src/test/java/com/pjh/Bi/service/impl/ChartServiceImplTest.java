package com.pjh.Bi.service.impl;

import com.pjh.Bi.service.ChartService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ChartServiceImplTest {

    @Resource
    private ChartService chartService;

    @Test
    void createTableForCsvData() {
        chartService.createTableForCsvData("日期,人数\n1,2\n2,3\n","12313123L");
    }
}