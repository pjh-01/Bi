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
        String input = "打击我1111111aaaaaaa\n" +
                "{\n" +
                "  \"tooltip\": {\n" +
                "    \"trigger\": \"axis\",\n" +
                "    \"axisPointer\": {\n" +
                "      \"type\": \"cross\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"legend\": {\n" +
                "    \"data\": [\"订单日期\", \"订单总金额\"]\n" +
                "  },\n" +
                "  \"xAxis\": {\n" +
                "    \"type\": \"category\",\n" +
                "    \"data\": [\"1/6/21\", \"1/23/21\", \"2/9/21\", \"2/26/21\", \"3/15/21\", \"4/1/21\", \"4/18/21\", \"5/5/21\", \"5/22/21\", \"6/8/21\", \"6/25/21\", \"7/12/21\", \"8/15/21\", \"9/1/21\", \"9/18/21\", \"10/5/21\", \"10/22/21\", \"11/8/21\", \"11/25/21\", \"12/12/21\", \"12/29/21\", \"1/15/22\", \"2/1/22\", \"3/7/22\", \"3/24/22\", \"4/10/22\", \"4/27/22\", \"5/14/22\", \"5/31/22\", \"6/17/22\", \"7/4/22\", \"7/21/22\", \"8/7/22\", \"8/24/22\", \"9/10/22\", \"9/27/22\", \"10/14/22\", \"10/31/22\", \"11/17/22\", \"12/4/22\", \"12/30/33\"]\n" +
                "  },\n" +
                "  \"yAxis\": {\n" +
                "    \"type\": \"value\"\n" +
                "  },\n" +
                "  \"series\": [\n" +
                "    {\n" +
                "      \"name\": \"订单日期\",\n" +
                "      \"type\": \"line\",\n" +
                "      \"data\": [\"6/8/21\", \"6/8/21\", \"6/8e68fk+UgGQAAAAABJRU5ErkJggg==\"] //这里需要替换为实际数据\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"订单总金额\",\n" +
                "      \"type\": \"line\",\n" +
                "      \"data\": [39.85, 89.9, 999.5, 449.0, 539.4, 60, 89.9, 539.4, 449.0, 76, 9.0] //这里需要替换为实际数据\n" +
                "    }\n" +
                "  ]\n" +
                "}"+
                "大啊伟大伟大1111120aaaaaaaaaaa";

        String filteredJson = filterJson(input);
        System.out.println(filteredJson);
    }

    public static String filterJson(String input) {
        // 匹配JSON对象，包括前后可能的垃圾信息
        Pattern jsonPattern = Pattern.compile("\\{(?:[^{}]|\\{(?:[^{}]|\\{[^{}]*\\})*\\})*\\}", Pattern.DOTALL);
        Matcher jsonMatcher = jsonPattern.matcher(input);

        // 找到第一个匹配项
        if (jsonMatcher.find()) {
            return jsonMatcher.group();
        }

        // 如果没有匹配项，返回空字符串或根据需求返回其他值
        return "";
    }
}
