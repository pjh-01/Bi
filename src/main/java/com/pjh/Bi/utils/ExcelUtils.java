package com.pjh.Bi.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.pjh.Bi.common.ErrorCode;
import com.pjh.Bi.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class ExcelUtils {
    public static String excelToCsv(MultipartFile multipartFile) {
        List<LinkedHashMap<Integer, String>> list = null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (Exception e) {
            log.error("excel处理出现错误！", e);
        }
        if (CollectionUtil.isEmpty(list)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        //转换为csv,builder线程不安全，不过无所谓好吧~
        StringBuilder stringBuilder = new StringBuilder();
        for (LinkedHashMap<Integer, String> map : list) {
            List<String> data = map.values().stream()
                    .filter(ObjectUtils::isNotEmpty)//过滤掉null，空被视为有意义.对于StrUtil（hutool）Empty只判断null和空字符串，但Blank还将不可见字符也视为空
                    .collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(data,",")).append("\n");
        }
        return stringBuilder.toString();

    }

    public static void main(String[] args) {
        String input = "dajiwodjiaowdi嗡嗡嗡\n" +
                "{\n" +
                "  \"title\": {\n" +
                "    \"text\": \"访客数柱状图\",\n" +
                "    \"subtext\": \"数据来源： xxx\"\n" +
                "  },\n" +
                "  \"tooltip\": {\n" +
                "    \"trigger\": \"axis\"\n" +
                "  },\n" +
                "  \"legend\": {\n" +
                "    \"data\": [\"访客数\"]\n" +
                "  },\n" +
                "  \"xAxis\": {\n" +
                "    \"type\": \"category\",\n" +
                "    \"data\": [\"1号\", \"2号\", \"3号\"]\n" +
                "  },\n" +
                "  \"yAxis\": {\n" +
                "    \"type\": \"value\"\n" +
                "  },\n" +
                "  \"series\": [\n" +
                "    {\n" +
                "      \"name\": \"访客数\",\n" +
                "      \"type\": \"bar\",\n" +
                "      \"data\": [10, 20, 30]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "aidjia娃娃大大";

        // 使用正则表达式提取 JSON 数据
        String jsonPattern = "\\{.*\\}";
        Pattern pattern = Pattern.compile(jsonPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String extractedJson = matcher.group();
            System.out.println(extractedJson);
        } else {
            System.out.println("未找到匹配的 JSON 数据");
        }
    }
}
