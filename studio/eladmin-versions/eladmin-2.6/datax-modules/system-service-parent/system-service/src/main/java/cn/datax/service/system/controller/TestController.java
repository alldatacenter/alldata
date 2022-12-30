package cn.datax.service.system.controller;

import cn.datax.common.base.BaseController;
import cn.datax.common.base.DataScope;
import cn.datax.common.core.R;
import cn.datax.common.jasperreport.utils.JasperReportUtil;
import cn.datax.service.system.api.entity.UserEntity;
import cn.datax.service.system.api.query.UserQuery;
import cn.datax.service.system.service.UserService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController extends BaseController {

    @Autowired
    private UserService userService;

    @GetMapping("/users/pageDataScope")
    public R getUserPageDataScope(UserQuery userQuery) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(userQuery.getUsername()), "username", userQuery.getUsername());
        queryWrapper.eq(StrUtil.isNotBlank(userQuery.getDeptId()), "dept_id", userQuery.getDeptId());
        if(CollUtil.isNotEmpty(userQuery.getOrderList())){
            userQuery.getOrderList().stream().forEach(orderItem -> {
                queryWrapper.orderBy(StrUtil.isNotBlank(orderItem.getColumn()), orderItem.isAsc(), orderItem.getColumn());
            });
        }
        IPage<UserEntity> page = userService.pageDataScope(new Page<>(userQuery.getPageNum(), userQuery.getPageSize()), queryWrapper, new DataScope("dept_id", "create_by"));
        return R.ok().setData(page);
    }

    @GetMapping("/jasper/{type}")
    public void getReport(@PathVariable("type") String reportType, HttpServletResponse response) throws Exception {
        HashMap<String, Object> parameters = new HashMap<>();
        List<Map> teachList = new ArrayList<>();
        Map t1=new HashMap();
        t1.put("name","张老师");
        t1.put("job","语文老师");
        teachList.add(t1);
        Map t2=new HashMap();
        t2.put("name","王老师");
        t2.put("job","数学老师");
        teachList.add(t2);
        Map t3=new HashMap();
        t3.put("name","刘老师");
        t3.put("job","英语老师");
        teachList.add(t3);
        parameters.put("teachList", teachList);
        List<HashMap> list = new ArrayList<>();
        for (int i = 1; i < 6; i++) {
            HashMap<String, Object> item = new HashMap<>();
            item.put("grade",  i+"年级学生信息:");
            List<Map> studentList=new ArrayList<>();
            Map s1=new HashMap();
            s1.put("name","张"+i);
            s1.put("age",i+"");
            s1.put("sex",i%2==0?"男":"女");
            studentList.add(s1);
            Map s2=new HashMap();
            s2.put("name","王"+i);
            s2.put("age",i+"");
            s2.put("sex",i%2==0?"男":"女");
            studentList.add(s2);
            Map s3=new HashMap();
            s3.put("name","刘"+i);
            s3.put("age",i+"");
            s3.put("sex",i%2==0?"男":"女");
            studentList.add(s3);
            item.put("studentList",  studentList);
            list.add(item);
        }
        String jasperPath = JasperReportUtil.getJasperFileDir("report");
        if (reportType.equals("pdf")) {
            JasperReportUtil.exportToPdf(jasperPath, parameters, list, response);
        } else if (reportType.equals("html")) {
            JasperReportUtil.exportToHtml(jasperPath, parameters, list, response);
        }
    }
}
