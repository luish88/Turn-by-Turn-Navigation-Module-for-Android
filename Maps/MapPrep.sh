#!/bin/bash
echo "Which continent do you want to download maps from?"
read CONTINENT
echo "Which country do you want to download maps from?"
read COUNTRY

# adjusting continent names to match urls
case $CONTINENT in
	"australia" | "oceania" | "australia and oceania" | "australia-oceania")
		GEOFABRIK_CONTINENT="australia-oceania"
		MAPSFORGE_CONTINENT="australia-oceania"
		break
		;;

	"north america")
		GEOFABRIK_CONTINENT="north-america"
		MAPSFORGE_CONTINENT="north-america"
		break
		;;

	"central america")
		GEOFABRIK_CONTINENT="central-america"
		MAPSFORGE_CONTINENT="central-america"
		break
		;;

	"south america")
		GEOFABRIK_CONTINENT="south-america"
		MAPSFORGE_CONTINENT="south-america"
		break
		;;

	*)
		GEOFABRIK_CONTINENT=$CONTINENT
		MAPSFORGE_CONTINENT=$CONTINENT
		;;
esac

echo "Downloading maps from $CONTINENT: $COUNTRY"
echo "Downloading graph file from http://download.geofabrik.de/$GEOFABRIK_CONTINENT/$COUNTRY"
wget http://download.geofabrik.de/$GEOFABRIK_CONTINENT/$COUNTRY-latest.osm.pbf
echo "Downloading map tiles from http://download.mapsforge.org/maps/v4/$MAPSFORGE_CONTINENT/$COUNTRY"
wget http://download.mapsforge.org/maps/v4/$MAPSFORGE_CONTINENT/$COUNTRY.map
echo "-------------------- Download complete --------------------"
echo ".......Renaming files"
mv $COUNTRY-latest.osm.pbf $COUNTRY.osm
echo ".......Processing maps"
./graphhopper.sh import $COUNTRY.osm
mv $COUNTRY.map ./$COUNTRY-gh/$COUNTRY.map
cd $COUNTRY-gh
sudo zip -r ./$COUNTRY.ghz ./* #zipping all files
mv $COUNTRY.ghz ../
cd ..
echo ".......File $COUNTRY-ghz is ready. Would you like to remove the other files? (Y or N)"
read DELETEOPTION
if [ $DELETEOPTION = "Y" ] || [ $DELETEOPTION = "y" ] ; then
	echo "Folder $COUNTRY-gh was deleted"
	sudo rm -rf $COUNTRY-gh
	sudo rm -rf $COUNTRY.osm
else
	echo "Folder $COUNTRY-gh was not deleted"
fi
echo "-------------------- Map processing complete --------------------"

