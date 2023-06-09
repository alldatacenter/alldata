#grep -r -l 'searchtext' . | sort | uniq | xargs perl -e "s/matchtext/replacetext/" -pi
for f in `find /Users/mozhenghua/j2ee_solution/project/plugins  -name '*.tpi' -print`
do
   echo " ln -s $f "
   ln -s $f /opt/data/tis/libs/plugins/${f##*/}
done ;
