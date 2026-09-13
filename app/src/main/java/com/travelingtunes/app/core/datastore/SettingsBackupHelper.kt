package com.travelingtunes.app.core.datastore

import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.GestureBinding
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.core.model.ThemeSettings

object SettingsBackupHelper {
    fun exportToJson(
        display: DisplaySettings,
        theme: ThemeSettings,
        bindings: Map<GestureTrigger, GestureBinding>,
        gpsVolume: Boolean,
        gpsSens: Float,
        autoRescan: Boolean
    ): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": 1,\n")
        sb.append("  \"timestamp\": ${System.currentTimeMillis()},\n")

        // Display
        sb.append("  \"displaySettings\": {\n")
        sb.append("    \"artistFontSize\": ${display.artistFontSize},\n")
        sb.append("    \"songFontSize\": ${display.songFontSize},\n")
        sb.append("    \"albumFontSize\": ${display.albumFontSize},\n")
        sb.append("    \"artistAlignment\": \"${display.artistAlignment.name}\",\n")
        sb.append("    \"songAlignment\": \"${display.songAlignment.name}\",\n")
        sb.append("    \"albumAlignment\": \"${display.albumAlignment.name}\",\n")
        sb.append("    \"minimumFontSize\": ${display.minimumFontSize},\n")
        sb.append("    \"titleShrinkInPortrait\": ${display.titleShrinkInPortrait},\n")
        sb.append("    \"titleShrinkLong\": ${display.titleShrinkLong},\n")
        sb.append("    \"titleScrollLong\": ${display.titleScrollLong},\n")
        sb.append("    \"showAlbumArt\": ${display.showAlbumArt},\n")
        sb.append("    \"albumArtColors\": ${display.albumArtColors},\n")
        sb.append("    \"albumArtScale\": \"${display.albumArtScale.name}\",\n")
        sb.append("    \"artAlignmentPortrait\": \"${display.artAlignmentPortrait.name}\",\n")
        sb.append("    \"artAlignmentLandscape\": \"${display.artAlignmentLandscape.name}\",\n")
        sb.append("    \"albumArtFade\": ${display.albumArtFade},\n")
        sb.append("    \"artDisplayLayout\": \"${display.artDisplayLayout.name}\",\n")
        sb.append("    \"hudType\": \"${display.hudType.name}\",\n")
        sb.append("    \"scrubHudType\": \"${display.scrubHudType.name}\",\n")
        sb.append("    \"volumeAlwaysOn\": ${display.volumeAlwaysOn},\n")
        sb.append("    \"showStatusBar\": ${display.showStatusBar},\n")
        sb.append("    \"showActions\": ${display.showActions},\n")
        sb.append("    \"hudLineThickness\": ${display.hudLineThickness},\n")
        sb.append("    \"artistFontKey\": \"${display.artistFontKey}\",\n")
        sb.append("    \"songFontKey\": \"${display.songFontKey}\",\n")
        sb.append("    \"albumFontKey\": \"${display.albumFontKey}\",\n")
        sb.append("    \"artistBold\": ${display.artistBold},\n")
        sb.append("    \"artistItalic\": ${display.artistItalic},\n")
        sb.append("    \"artistUnderline\": ${display.artistUnderline},\n")
        sb.append("    \"songBold\": ${display.songBold},\n")
        sb.append("    \"songItalic\": ${display.songItalic},\n")
        sb.append("    \"songUnderline\": ${display.songUnderline},\n")
        sb.append("    \"albumBold\": ${display.albumBold},\n")
        sb.append("    \"albumItalic\": ${display.albumItalic},\n")
        sb.append("    \"albumUnderline\": ${display.albumUnderline},\n")
        sb.append("    \"keepScreenOn\": ${display.keepScreenOn},\n")
        sb.append("    \"immersiveMode\": ${display.immersiveMode},\n")
        sb.append("    \"numEdgeRegions\": ${display.numEdgeRegions},\n")
        sb.append("    \"titleOrder\": \"${display.titleOrder.joinToString(",") { it.name }}\",\n")
        sb.append("    \"autoCategoryOrder\": \"${display.autoCategoryOrder.joinToString(",") { it.name }}\",\n")
        sb.append("    \"autoShowAlbumArt\": ${display.autoShowAlbumArt},\n")
        sb.append("    \"autoAlbumStyleGrid\": ${display.autoAlbumStyleGrid},\n")
        sb.append("    \"autoArtistStyleGrid\": ${display.autoArtistStyleGrid},\n")
        sb.append("    \"autoAutoplayOnConnect\": ${display.autoAutoplayOnConnect},\n")
        sb.append("    \"autoVoiceSearch\": ${display.autoVoiceSearch},\n")
        sb.append("    \"autoSpeedVolumeEnabled\": ${display.autoSpeedVolumeEnabled},\n")
        sb.append("    \"autoDefaultVolume\": ${display.autoDefaultVolume},\n")
        sb.append("    \"autoMinSpeedThreshold\": ${display.autoMinSpeedThreshold},\n")
        sb.append("    \"autoSpeedVolumeRatio\": ${display.autoSpeedVolumeRatio},\n")
        sb.append("    \"autoSpeedUnit\": \"${display.autoSpeedUnit}\",\n")
        sb.append("    \"drivingModeEnabled\": ${display.drivingModeEnabled},\n")
        sb.append("    \"autoEnableDrivingMode\": ${display.autoEnableDrivingMode},\n")
        sb.append("    \"autoActionButtonOrder\": \"${display.autoActionButtonOrder.joinToString(",") { it.name }}\"\n")
        sb.append("  },\n")

        // Theme
        sb.append("  \"themeSettings\": {\n")
        sb.append("    \"currentThemeName\": \"${theme.currentThemeName}\",\n")
        sb.append("    \"customTextRed\": ${theme.customTextRed},\n")
        sb.append("    \"customTextGreen\": ${theme.customTextGreen},\n")
        sb.append("    \"customTextBlue\": ${theme.customTextBlue},\n")
        sb.append("    \"customSongTitleRed\": ${theme.customSongTitleRed},\n")
        sb.append("    \"customSongTitleGreen\": ${theme.customSongTitleGreen},\n")
        sb.append("    \"customSongTitleBlue\": ${theme.customSongTitleBlue},\n")
        sb.append("    \"customArtistTitleRed\": ${theme.customArtistTitleRed},\n")
        sb.append("    \"customArtistTitleGreen\": ${theme.customArtistTitleGreen},\n")
        sb.append("    \"customArtistTitleBlue\": ${theme.customArtistTitleBlue},\n")
        sb.append("    \"customAlbumTitleRed\": ${theme.customAlbumTitleRed},\n")
        sb.append("    \"customAlbumTitleGreen\": ${theme.customAlbumTitleGreen},\n")
        sb.append("    \"customAlbumTitleBlue\": ${theme.customAlbumTitleBlue},\n")
        sb.append("    \"customBGRed\": ${theme.customBGRed},\n")
        sb.append("    \"customBGGreen\": ${theme.customBGGreen},\n")
        sb.append("    \"customBGBlue\": ${theme.customBGBlue},\n")
        sb.append("    \"dimAtNight\": ${theme.dimAtNight},\n")
        sb.append("    \"invertAtNight\": ${theme.invertAtNight},\n")
        sb.append("    \"sunRiseHour\": ${theme.sunRiseHour},\n")
        sb.append("    \"sunSetHour\": ${theme.sunSetHour},\n")
        sb.append("    \"isRounded\": ${theme.isRounded},\n")
        sb.append("    \"isGlass\": ${theme.isGlass}\n")
        sb.append("  },\n")

        // Gestures
        sb.append("  \"gestureBindings\": {\n")
        val bindingsList = bindings.entries.toList()
        bindingsList.forEachIndexed { index, (trigger, binding) ->
            val comma = if (index < bindingsList.size - 1) "," else ""
            sb.append("    \"${trigger.key}\": { \"action\": \"${binding.action.name}\", \"isContinuous\": ${binding.isContinuous} }$comma\n")
        }
        sb.append("  },\n")

        // Other
        sb.append("  \"gpsVolumeEnabled\": $gpsVolume,\n")
        sb.append("  \"gpsSensitivity\": $gpsSens,\n")
        sb.append("  \"autoRescan\": $autoRescan\n")
        sb.append("}")
        return sb.toString()
    }
}
