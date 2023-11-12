# Document For AllData

## build document 
> 1 pip3 install -U Sphinx
> 
> 2 install dependency
> 
```markdown
pip3 install sphinx-autobuild
pip3 install sphinx_rtd_theme
pip3 install --upgrade myst-parser
pip3 install sphinx_markdown_tables
```
> 3 配置mac(mac单独配置)
```markdown
echo 'export HOMEBREW_BOTTLE_DOMAIN=https://mirrors.tuna.tsinghua.edu.cn/homebrew-bottles' >> ~/.bash_profile
source ~/.bash_profile
```
> 4 brew install sphinx-doc
> 
> 5 echo 'export PATH="/opt/homebrew/opt/sphinx-doc/libexec/bin:$PATH"' >> ~/.zshrc
> 
> 6 sphinx-build --version  查看version
> 
> 7 python3.10 -m pip install --upgrade pip
> 
> 8  pip3 install --upgrade myst-parser && pip3 install sphinxawesome_theme
> 
> 9 make html
> 
> 10 双击访问本地alldata/document/build/html/index.html
> 
