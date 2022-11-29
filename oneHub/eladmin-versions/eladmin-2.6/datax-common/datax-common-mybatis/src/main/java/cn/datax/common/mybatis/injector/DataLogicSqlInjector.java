package cn.datax.common.mybatis.injector;

import cn.datax.common.mybatis.injector.methods.SelectListDataScope;
import cn.datax.common.mybatis.injector.methods.SelectPageDataScope;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;

import java.util.List;

/**
 * 自定义 SqlInjector
 */
public class DataLogicSqlInjector extends DefaultSqlInjector {

    /**
     * 如果只需增加方法，保留MP自带方法
     * 可以super.getMethodList() 再add
     * @return
     */
    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass) {
        List<AbstractMethod> methodList = super.getMethodList(mapperClass);
        methodList.add(new SelectListDataScope());
        methodList.add(new SelectPageDataScope());
        return methodList;
    }
}
