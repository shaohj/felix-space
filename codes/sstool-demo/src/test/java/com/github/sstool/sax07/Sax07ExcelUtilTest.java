package com.github.sstool.sax07;

import com.github.shaohj.sstool.core.util.CostTimeUtil;
import com.github.shaohj.sstool.poiexpand.sax07.Sax07ExcelExportParam;
import com.github.shaohj.sstool.poiexpand.sax07.Sax07ExcelUtil;
import com.github.shaohj.sstool.poiexpand.sax07.service.Sax07ExcelPageWriteService;
import com.github.sstool.sax07.entity.ModelTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.io.FileOutputStream;
import java.util.*;

/**
 * 编  号：
 * 名  称：Sax07ExcelUtilTest
 * 描  述：
 * 完成日期：2019/01/29 00:27
 *
 * @author：felix.shao
 */
@Slf4j
public class Sax07ExcelUtilTest {

    public static final String exportPath = "E:\\temp\\export\\";

    @Before
    public void before(){
        log.info("缓存导出的xlsx临时文件目录为:{}", System.getProperty("java.io.tmpdir"));
    }

    @Test
    public void exportSstoolTest(){
        int pageSize = 10;
        int pageForeachTotalNum = 5;
        int totalPageNum = pageForeachTotalNum % pageSize == 0 ? pageForeachTotalNum/pageSize : pageForeachTotalNum/pageSize + 1;
        // 使用类classpath路径下的模板文件导出
        String tempPath = "xlsx/";

        Map<String, Object> params = new HashMap<>();

        // 普通数据mock
        params.put("printDate", "2019-06-29");

        // each数据mock
        ModelTest eachModel = new ModelTest("张三", "zhangsan", 123.234);
        eachModel.setYear("2008");
        params.put("eachModel", eachModel);

        // foreach数据mock
        List forEachList = new ArrayList();
        for(int i = 0; i< 4; i++){
            forEachList.add(new ModelTest("姓名" + i, "user" + i, i));
        }
        params.put("forEachList", forEachList);

        // 分页导出数据mock
        Sax07ExcelPageWriteService sax07ExcelPageWriteService = new Sax07ExcelPageWriteService(){
            @Override
            public void pageWriteData() {
                for (int i = 0; i <totalPageNum; i++) {
                    Map<String, Object> pageParams = new HashMap<>();
                    List pageForeachList = new ArrayList(pageSize);
                    for (int j = 0; j <pageSize && pageSize * i + j < pageForeachTotalNum; j++) {
                        pageForeachList.add(new ModelTest("分页姓名" + i + "," + j, "分页user" + i + "," + j, j));
                    }
                    pageParams.put("pageForeachList", pageForeachList);
                    tagData.writeTagData(writeWb, writeSheet, writeSheetData, pageParams, writeCellStyleCache);
                }
            }
        };
        //设置sax07ExcelPageWriteService对应的表达式
        sax07ExcelPageWriteService.setExprVal("#pageforeach pageObj in ${pageForeachList}");

        CostTimeUtil.apply(null, "导出" + pageForeachTotalNum + "条数据,耗费时间为{}毫秒", s -> {
            try (FileOutputStream fos = new FileOutputStream(exportPath + "sstool_demo_data.xlsx");){
                Sax07ExcelExportParam param = Sax07ExcelExportParam.builder()
                        .tempIsClassPath(true)
                        .tempFileName(tempPath + "sstool_demo_temp.xlsx")
                        .params(params)
                        .sax07ExcelPageWriteServices(Arrays.asList(sax07ExcelPageWriteService))
                        .outputStream(fos).build();
                Sax07ExcelUtil.export(param);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
