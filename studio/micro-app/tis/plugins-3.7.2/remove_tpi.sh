
for f in `find .  -name '*.tpi' -print`
do
   echo " rm -f $f "
   rm -f $f
done ;
