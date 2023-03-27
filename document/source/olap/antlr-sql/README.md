# Antlr4 SQL POC

<br/>
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/20246692/227958337-611c8b86-bc99-4a42-ad56-49f99531dd39.png">
<br/>
<br/>
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/20246692/227958353-4ec1b51b-1514-4845-a441-4d689f5d6fd8.png">
<br/>
<br/>
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/20246692/227958367-9ae952f6-adf3-4bbc-8191-2619785ddb9f.png">
<br/>
<br/>
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/20246692/227958381-1650deff-6fe1-4cb2-ba39-5af04bb3cc1f.png">
<br/>
1、Antlr4词法解析和语法解析
> 包括词法解析、语法解析、Antlr4的结果的处理

2、Antlr4执行阶段
> 1. 分为Lexer和Parser，实际上表示了两个不同的阶段：
>
> 2. 词法分析阶段：对应于Lexer定义的词法规则，解析结果为一个一个的Token；
     > 解析阶段：根据词法，构造出来一棵解析树或者语法树。
>
> 3. 词法解析和语法解析的调和
> 4. 首先，语法解析相对于词法解析，会产生更多的开销，所以，应该尽量将某些可能的处理在词法解析阶段完成，减少语法解析阶段的开销
> 5. 合并语言不关心的标记，例如，某些语言（例如js）不区分int、double，只有 number，那么在词法解析阶段，
     > 就不需要将int和double区分开，统一合并为一个number；
> 6. 空格、注释等信息，对于语法解析并无大的帮助，可以在词法分析阶段剔除掉；
     > 诸如标志符、关键字、字符串和数字这样的常用记号，均应该在词法解析时完成，而不要到语法解析阶段再进行。
> 7. 只有 number，没有 int 和 double 等，但是面向静态代码分析，我们可能需要知道确切的类型来帮助分析特定的缺陷；
>
> 8. 虽然注释对代码帮助不大，但是我们有时候也需要解析注释的内容来进行分析，如果无法在语法解析的时候获取，
     > 那么就需要遍历Token，从而导致静态代码分析开销更大等；
> 9. 解析树vs语法树
> 10. Antlr4生成的树状结构，称为解析树或者是语法树，如果通过Antlr4解析语言简单使用，可以直接基于Antlr4的结果开发，
      但是如果要进行更加深入的处理，就需要对Antlr4的结果进行更进一步的处理，以更符合我们的使用习惯
> 11. Java Parser格式的Java的AST，Clang格式的C/C++的AST, 然后才能更好地在上面进行开发。
