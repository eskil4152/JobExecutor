package com.blikeng.job.executor.metadata.image;

import java.util.Set;

public final class ImageMetadataWhitelist {

    private ImageMetadataWhitelist() {
    }

    public static final Set<String> ALLOWED = Set.of(
            // Generic image dimensions
            "Exif SubIFD.Exif Image Width",
            "Exif SubIFD.Exif Image Height",
            "JPEG.Image Width",
            "JPEG.Image Height",
            "PNG-IHDR.Image Width",
            "PNG-IHDR.Image Height",
            "GIF Header.Image Width",
            "GIF Header.Image Height",
            "BMP Header.Image Width",
            "BMP Header.Image Height",
            "WebP.Image Width",
            "WebP.Image Height",

            // Camera / device
            "Exif IFD0.Make",
            "Exif IFD0.Model",
            "Exif IFD0.Software",

            // Photo timing / orientation
            "Exif SubIFD.Date/Time Original",
            "Exif IFD0.Orientation",

            // Photo settings
            "Exif SubIFD.Exposure Time",
            "Exif SubIFD.F-Number",
            "Exif SubIFD.ISO Speed Ratings",
            "Exif SubIFD.Focal Length",

            // Color / resolution
            "Exif SubIFD.Color Space",
            "Exif IFD0.X Resolution",
            "Exif IFD0.Y Resolution",
            "Exif IFD0.Resolution Unit",

            // GPS
            "GPS.GPS Latitude",
            "GPS.GPS Longitude",
            "GPS.GPS Altitude"
    );
}