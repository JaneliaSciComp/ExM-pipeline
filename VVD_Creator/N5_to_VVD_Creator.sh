#sh wrote by Hideo Otsuna

FIJI=/nrs/scicompsoft/otsuna/Macros/Fiji.appold/ImageJ-linux64

PREPROCIMG=/nrs/scicompsoft/otsuna/EM_test/ExM/N5_VVD_creator_cluster.ijm

Originaldir=$1
workdir=$2
PyramidLV=$3
FinalRatio=$4
MinThre=$5
MaxThre=$6

if [ ! -e $Pyramiddir]
then
mkdir $Pyramiddir
fi

if [ ! -e $VVDdir]
then
mkdir $VVDdir
fi

#number of CPU
numCPU=32



echo "Running on "`hostname`
echo "Finding a port for Xvfb, starting at $DISPLAY_PORT..."
PORT=$DISPLAY_PORT COUNTER=0 RETRIES=10
function cleanXvfb {
  kill $MYPID
  rm -f /tmp/.X${PORT}-lock
  rm -f /tmp/.X11-unix/X${PORT}
  echo "Cleaned up Xvfb"
}
trap cleanXvfb EXIT
while [ "$COUNTER" -lt "$RETRIES" ]; do
while (test -f "/tmp/.X${PORT}-lock") || (test -f "/tmp/.X11-unix/X${PORT}") || (netstat -atwn | grep "^.*:${PORT}.*:\*\s*LISTEN\s*$")
do PORT=$(( ${PORT} + 1 ))
done
echo "Found the first free port: $PORT"
/usr/bin/Xvfb :${PORT} -screen 0 1280x1024x24 -fp /usr/share/X11/fonts/misc > Xvfb.${PORT}.log 2>&1 &
echo "Started Xvfb on port $PORT"
MYPID=$!
export DISPLAY="localhost:${PORT}.0"
sleep 3
if kill -0 $MYPID >/dev/null 2>&1; then
echo "Xvfb is running as $MYPID"
break
else
echo "Xvfb died immediately, trying again..."
cleanXvfb
PORT=$(( ${PORT} + 1 ))
fi
COUNTER="$(( $COUNTER + 1 ))"
done

function exitHandler() { cleanXvfb; }
trap exitHandler EXIT

# RUN FIJI IN THE BACKGROUND HERE (put & at the end of the command)
cd $Originaldir
$FIJI -macro $PREPROCIMG "$Originaldir/,$workdir/,$numCPU,$PyramidLV,$FinalRatio,$MinThre,$MaxThre" &


fpid=$!
XVFB_SCREENSHOT_DIR="./xvfb.${PORT}"
mkdir -p $XVFB_SCREENSHOT_DIR
ssinc=30
freq=$ssinc
inc=5
t=0
nt=$freq
while kill -0 $fpid 2> /dev/null; do
sleep $inc
t=$((t+inc))
if [ "$t" -eq "$nt" ]; then
freq=$((freq+ssinc))
nt=$((t+freq))
DISPLAY=:$PORT import -window root $XVFB_SCREENSHOT_DIR/screenshot_$t.png
fi
if [ "$t" -gt 72000 ]; then
echo "Killing Xvfb session which has been running for over 72000 seconds"
kill -9 $fpid 2> /dev/null
fi
done
