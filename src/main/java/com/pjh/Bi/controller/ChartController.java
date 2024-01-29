package com.pjh.Bi.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pjh.Bi.annotation.AuthCheck;
import com.pjh.Bi.common.BaseResponse;
import com.pjh.Bi.common.DeleteRequest;
import com.pjh.Bi.common.ErrorCode;
import com.pjh.Bi.common.ResultUtils;
import com.pjh.Bi.constant.UserConstant;
import com.pjh.Bi.exception.BusinessException;
import com.pjh.Bi.exception.ThrowUtils;
import com.pjh.Bi.manager.RedisLimiterManager;
import com.pjh.Bi.model.dto.chart.ChartAddRequest;
import com.pjh.Bi.model.entity.Chart;
import com.pjh.Bi.model.entity.User;
import com.pjh.Bi.model.vo.BiResponse;
import com.pjh.Bi.service.ChartService;
import com.pjh.Bi.service.UserService;
import com.pjh.Bi.constant.CommonConstant;
import com.pjh.Bi.model.dto.chart.*;
import com.pjh.Bi.utils.AiUtils;
import com.pjh.Bi.utils.ExcelUtils;
import com.pjh.Bi.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;


    @Resource
    private RedisLimiterManager redisLimiterManager;

//    @Resource
//    private ThreadPoolExecutor threadPoolExecutor;

//    @Resource
//    private BiMessageProducer biMessageProducer;


    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }


    public boolean checkForAi(String str) {
        return !(StringUtils.isNotBlank(str) && str.length() <= 100);
    }

    /**
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) throws Exception {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(checkForAi(goal), ErrorCode.PARAMS_ERROR, "目标为空或过长");
        ThrowUtils.throwIf(checkForAi(name), ErrorCode.PARAMS_ERROR, "名称为空或过长");
        ThrowUtils.throwIf(checkForAi(chartType), ErrorCode.PARAMS_ERROR, "类型为空或过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");
        // 校验文件后缀 aaa.png
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        User loginUser = userService.getLoginUser(request);
        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
        // 定义prompt
        final String prompt = "你是一个数据分析师和前端开发专家，你需要分析以下csv数据，回答" + goal + ",并给出不少于200字的分析结论和用于生成echarts" + chartType + "option的json,你的json不能包含任何注释！";
        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        //注入prompt
        userInput.append(prompt).append("\n");
        //提供要分析的数据
        userInput.append("需要分析的数据：").append("\n");
        // 压缩后的数据:为什么要压缩？节约token，增加提问能力（长短）
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");
        //指定回答的格式
        final String howToAnswer = "你的回答格式必须严格为：\n" +
                "---pjh---\n" +
                "分析结论\n" +
                "---pjh---\n" +
                "//json格式的option";
        userInput.append(howToAnswer).append("\n");
        String result = AiUtils.ask(userInput.toString());
        String[] splits = result.split("---pjh---");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }
        String genResult = splits[1].trim();
        String genChart = splits[2].trim();
        //过滤垃圾，只需要提取json
        // 使用正则表达式提取 JSON 数据
        String jsonPattern = "\\{.*\\}";
        Pattern pattern = Pattern.compile(jsonPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(genChart);
        if (matcher.find()) {
            String extractedJson = matcher.group();
            System.out.println(extractedJson);

            genChart = extractedJson;
        } else {
            System.out.println("未找到匹配的 JSON 数据");
        }
        // 插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        //这边需要改造一下，不能直接把数据存在表的字段里面了。不然数据多了查询就非常慢
        //1.比如别的用户存了100M的数据，我虽然用不到，但是数据库还得全局搜索，搜到他的多少会影响速度
        //2.还有100M的数据，用户可能不需要看这么多，只需要对比其中几列就好了，但字段肯定得全部读出来，这又很慢
//        chart.setChartData(csvData);
        String uuid = IdUtil.simpleUUID().substring(0, 6);
        String chartId = "_" + uuid + "_" + loginUser.getId();
        //在原chart表中存储寻找独表的索引，由独表的uuid和userId组成
        chart.setChartData(chartId);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        chart.setStatus("succeed");
        boolean saveResult = chartService.save(chart);
        //生成独表
        chartService.createTableForCsvData(csvData, chartId);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);

    }

//    /**
//     * 智能分析（异步）
//     *
//     * @param multipartFile
//     * @param genChartByAiRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/gen/async")
//    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
//                                             GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
//        String name = genChartByAiRequest.getName();
//        String goal = genChartByAiRequest.getGoal();
//        String chartType = genChartByAiRequest.getChartType();
//        // 校验
//        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
//        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
//        // 校验文件
//        long size = multipartFile.getSize();
//        String originalFilename = multipartFile.getOriginalFilename();
//        // 校验文件大小
//        final long ONE_MB = 1024 * 1024L;
//        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");
//        // 校验文件后缀 aaa.png
//        String suffix = FileUtil.getSuffix(originalFilename);
//        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
//        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");
//
//        User loginUser = userService.getLoginUser(request);
//        // 限流判断，每个用户一个限流器
//        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
//        // 无需写 prompt，直接调用现有模型，https://www.yucongming.com，公众号搜【鱼聪明AI】
////        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
////                "分析需求：\n" +
////                "{数据分析的需求或者目标}\n" +
////                "原始数据：\n" +
////                "{csv格式的原始数据，用,作为分隔符}\n" +
////                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
////                "【【【【【\n" +
////                "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
////                "【【【【【\n" +
////                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";
//        long biModelId = 1659171950288818178L;
//        // 分析需求：
//        // 分析网站用户的增长情况
//        // 原始数据：
//        // 日期,用户数
//        // 1号,10
//        // 2号,20
//        // 3号,30
//
//        // 构造用户输入
//        StringBuilder userInput = new StringBuilder();
//        userInput.append("分析需求：").append("\n");
//
//        // 拼接分析目标
//        String userGoal = goal;
//        if (StringUtils.isNotBlank(chartType)) {
//            userGoal += "，请使用" + chartType;
//        }
//        userInput.append(userGoal).append("\n");
//        userInput.append("原始数据：").append("\n");
//        // 压缩后的数据
//        String csvData = ExcelUtils.excelToCsv(multipartFile);
//        userInput.append(csvData).append("\n");
//
//        // 插入到数据库
//        Chart chart = new Chart();
//        chart.setName(name);
//        chart.setGoal(goal);
//        chart.setChartData(csvData);
//        chart.setChartType(chartType);
//        chart.setStatus("wait");
//        chart.setUserId(loginUser.getId());
//        boolean saveResult = chartService.save(chart);
//        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
//
//        // todo 建议处理任务队列满了后，抛异常的情况
//        CompletableFuture.runAsync(() -> {
//            // 先修改图表任务状态为 “执行中”。等执行成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。
//            Chart updateChart = new Chart();
//            updateChart.setId(chart.getId());
//            updateChart.setStatus("running");
//            boolean b = chartService.updateById(updateChart);
//            if (!b) {
//                handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
//                return;
//            }
//            // 调用 AI
//            String result = aiManager.doChat(biModelId, userInput.toString());
//            String[] splits = result.split("【【【【【");
//            if (splits.length < 3) {
//                handleChartUpdateError(chart.getId(), "AI 生成错误");
//                return;
//            }
//            String genChart = splits[1].trim();
//            String genResult = splits[2].trim();
//            Chart updateChartResult = new Chart();
//            updateChartResult.setId(chart.getId());
//            updateChartResult.setGenChart(genChart);
//            updateChartResult.setGenResult(genResult);
//            // todo 建议定义状态为枚举值
//            updateChartResult.setStatus("succeed");
//            boolean updateResult = chartService.updateById(updateChartResult);
//            if (!updateResult) {
//                handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
//            }
//        }, threadPoolExecutor);
//
//        BiResponse biResponse = new BiResponse();
//        biResponse.setChartId(chart.getId());
//        return ResultUtils.success(biResponse);
//    }

//    /**
//     * 智能分析（异步消息队列）
//     *
//     * @param multipartFile
//     * @param genChartByAiRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/gen/async/mq")
//    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
//                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
//        String name = genChartByAiRequest.getName();
//        String goal = genChartByAiRequest.getGoal();
//        String chartType = genChartByAiRequest.getChartType();
//        // 校验
//        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
//        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
//        // 校验文件
//        long size = multipartFile.getSize();
//        String originalFilename = multipartFile.getOriginalFilename();
//        // 校验文件大小
//        final long ONE_MB = 1024 * 1024L;
//        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");
//        // 校验文件后缀 aaa.png
//        String suffix = FileUtil.getSuffix(originalFilename);
//        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
//        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");
//
//        User loginUser = userService.getLoginUser(request);
//        // 限流判断，每个用户一个限流器
//        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
//        // 无需写 prompt，直接调用现有模型，https://www.yucongming.com，公众号搜【鱼聪明AI】
////        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
////                "分析需求：\n" +
////                "{数据分析的需求或者目标}\n" +
////                "原始数据：\n" +
////                "{csv格式的原始数据，用,作为分隔符}\n" +
////                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
////                "【【【【【\n" +
////                "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
////                "【【【【【\n" +
////                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";
//        long biModelId = 1659171950288818178L;
//        // 分析需求：
//        // 分析网站用户的增长情况
//        // 原始数据：
//        // 日期,用户数
//        // 1号,10
//        // 2号,20
//        // 3号,30
//
//        // 构造用户输入
//        StringBuilder userInput = new StringBuilder();
//        userInput.append("分析需求：").append("\n");
//
//        // 拼接分析目标
//        String userGoal = goal;
//        if (StringUtils.isNotBlank(chartType)) {
//            userGoal += "，请使用" + chartType;
//        }
//        userInput.append(userGoal).append("\n");
//        userInput.append("原始数据：").append("\n");
//        // 压缩后的数据
//        String csvData = ExcelUtils.excelToCsv(multipartFile);
//        userInput.append(csvData).append("\n");
//
//        // 插入到数据库
//        Chart chart = new Chart();
//        chart.setName(name);
//        chart.setGoal(goal);
//        chart.setChartData(csvData);
//        chart.setChartType(chartType);
//        chart.setStatus("wait");
//        chart.setUserId(loginUser.getId());
//        boolean saveResult = chartService.save(chart);
//        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
//        long newChartId = chart.getId();
//        biMessageProducer.sendMessage(String.valueOf(newChartId));
//        BiResponse biResponse = new BiResponse();
//        biResponse.setChartId(newChartId);
//        return ResultUtils.success(biResponse);
//    }


    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
//        updateChartResult.setStatus("failed");
//        updateChartResult.setExecMessage("execMessage");
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }


    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


}
