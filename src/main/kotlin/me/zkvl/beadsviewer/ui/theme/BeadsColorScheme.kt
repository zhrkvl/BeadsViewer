package me.zkvl.beadsviewer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Defines the color scheme for the Beads Viewer theme.
 * Provides semantic color tokens with proper contrast ratios for accessibility.
 */
data class BeadsColorScheme(
    // Surface colors
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceHover: Color,
    val surfaceSelected: Color,

    // Content colors
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val onSurfaceDisabled: Color,

    // Primary colors
    val primary: Color,
    val onPrimary: Color,
    val primaryVariant: Color,

    // Border and divider colors
    val border: Color,
    val borderStrong: Color,
    val divider: Color,

    // State colors
    val error: Color,
    val warning: Color,
    val success: Color,
    val info: Color,

    // Status colors (semantic)
    val statusOpen: Color,
    val statusInProgress: Color,
    val statusBlocked: Color,
    val statusClosed: Color,
    val statusDeferred: Color,
    val statusHooked: Color,

    // Priority colors (semantic)
    val priorityP0: Color,
    val priorityP1: Color,
    val priorityP2: Color,
    val priorityP3: Color,
    val priorityP4: Color,

    // Type colors (semantic)
    val typeTask: Color,
    val typeBug: Color,
    val typeFeature: Color,
    val typeEpic: Color,
    val typeChore: Color,

    // Query syntax highlighting colors
    val syntaxKeyword: Color,
    val syntaxField: Color,
    val syntaxString: Color,
    val syntaxNumber: Color,
    val syntaxValue: Color,
    val syntaxOperator: Color,
)

/**
 * Dark color scheme for the Beads Viewer.
 * Follows IntelliJ Darcula theme conventions with enhanced colors.
 */
val DarkColorScheme = BeadsColorScheme(
    // Surface colors
    background = Color(0xFF2B2B2B),
    surface = Color(0xFF3C3F41),
    surfaceVariant = Color(0xFF313335),
    surfaceHover = Color(0x08FFFFFF),
    surfaceSelected = Color(0x10FFFFFF),

    // Content colors
    onBackground = Color(0xFFCCCCCC),
    onSurface = Color(0xFFCCCCCC),
    onSurfaceVariant = Color(0xFFAAAAAA),
    onSurfaceDisabled = Color(0xFF888888),

    // Primary colors
    primary = Color(0xFF5C9FE5),
    onPrimary = Color(0xFFFFFFFF),
    primaryVariant = Color(0xFF4A8CD4),

    // Border and divider colors
    border = Color(0xFF3C3F41),
    borderStrong = Color(0xFF555555),
    divider = Color(0xFF2B2B2B),

    // State colors
    error = Color(0xFFFF5555),
    warning = Color(0xFFE5A55C),
    success = Color(0xFF5CE585),
    info = Color(0xFF5C9FE5),

    // Status colors (semantic)
    statusOpen = Color(0xFF5C9FE5),
    statusInProgress = Color(0xFFE5A55C),
    statusBlocked = Color(0xFFE55C5C),
    statusClosed = Color(0xFF5CE585),
    statusDeferred = Color(0xFF9E9E9E),
    statusHooked = Color(0xFFB85CE5),

    // Priority colors (semantic)
    priorityP0 = Color(0xFFDB3737),
    priorityP1 = Color(0xFFE5825C),
    priorityP2 = Color(0xFFE5C55C),
    priorityP3 = Color(0xFF85B8E5),
    priorityP4 = Color(0xFF9E9E9E),

    // Type colors (semantic)
    typeTask = Color(0xFF5C9FE5),
    typeBug = Color(0xFFE55C5C),
    typeFeature = Color(0xFF5CE585),
    typeEpic = Color(0xFFB85CE5),
    typeChore = Color(0xFF9E9E9E),

    // Query syntax highlighting colors
    syntaxKeyword = Color(0xFFCC7832),
    syntaxField = Color(0xFF9876AA),
    syntaxString = Color(0xFF6A8759),
    syntaxNumber = Color(0xFF6897BB),
    syntaxValue = Color(0xFFA9B7C6),
    syntaxOperator = Color(0xFFCCCCCC),
)

/**
 * Light color scheme for the Beads Viewer.
 * Follows IntelliJ Light theme conventions with enhanced colors.
 */
val LightColorScheme = BeadsColorScheme(
    // Surface colors
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F0F0),
    surfaceHover = Color(0x08000000),
    surfaceSelected = Color(0x10000000),

    // Content colors
    onBackground = Color(0xFF2B2B2B),
    onSurface = Color(0xFF2B2B2B),
    onSurfaceVariant = Color(0xFF555555),
    onSurfaceDisabled = Color(0xFF999999),

    // Primary colors
    primary = Color(0xFF2B7FDB),
    onPrimary = Color(0xFFFFFFFF),
    primaryVariant = Color(0xFF1E6CC2),

    // Border and divider colors
    border = Color(0xFFCCCCCC),
    borderStrong = Color(0xFF999999),
    divider = Color(0xFFE0E0E0),

    // State colors
    error = Color(0xFFE53935),
    warning = Color(0xFFE5911C),
    success = Color(0xFF3F9E4F),
    info = Color(0xFF2B7FDB),

    // Status colors (semantic) - darker versions for light mode
    statusOpen = Color(0xFF2B7FDB),
    statusInProgress = Color(0xFFE5911C),
    statusBlocked = Color(0xFFE53935),
    statusClosed = Color(0xFF3F9E4F),
    statusDeferred = Color(0xFF757575),
    statusHooked = Color(0xFF9C27B0),

    // Priority colors (semantic) - darker versions for light mode
    priorityP0 = Color(0xFFC62828),
    priorityP1 = Color(0xFFE56020),
    priorityP2 = Color(0xFFD4A61C),
    priorityP3 = Color(0xFF3D8CD4),
    priorityP4 = Color(0xFF757575),

    // Type colors (semantic) - darker versions for light mode
    typeTask = Color(0xFF2B7FDB),
    typeBug = Color(0xFFE53935),
    typeFeature = Color(0xFF3F9E4F),
    typeEpic = Color(0xFF9C27B0),
    typeChore = Color(0xFF757575),

    // Query syntax highlighting colors - adapted for light background
    syntaxKeyword = Color(0xFFCC7832),
    syntaxField = Color(0xFF9876AA),
    syntaxString = Color(0xFF6A8759),
    syntaxNumber = Color(0xFF1750EB),
    syntaxValue = Color(0xFF555555),
    syntaxOperator = Color(0xFF2B2B2B),
)
