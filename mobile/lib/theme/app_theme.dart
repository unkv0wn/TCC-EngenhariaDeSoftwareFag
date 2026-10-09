import 'package:flutter/material.dart';

/// Paleta do RouteWise.
///
/// Espelha os tokens de `FrontEnd-Next/app/globals.css`. São duas verdades em
/// linguagens diferentes: mudar a marca exige mudar os dois arquivos.
abstract final class AppColors {
  static const primary500 = Color(0xFF8B5CF6);
  static const primary600 = Color(0xFF7C3AED);
  static const primary700 = Color(0xFF6D28D9);
  static const primary50 = Color(0xFFF5F3FF);
  static const primary100 = Color(0xFFEDE9FE);
  static const primary200 = Color(0xFFDDD6FE);

  static const success600 = Color(0xFF059669);
  static const success700 = Color(0xFF047857);
  static const success50 = Color(0xFFECFDF5);

  static const danger600 = Color(0xFFDC2626);
  static const danger700 = Color(0xFFB91C1C);
  static const danger50 = Color(0xFFFEF2F2);
  static const danger200 = Color(0xFFFECACA);

  static const warning500 = Color(0xFFF59E0B);
  static const warning50 = Color(0xFFFFFBEB);
  static const warning200 = Color(0xFFFDE68A);
  static const warning800 = Color(0xFF92400E);

  static const secondary50 = Color(0xFFF0F9FF);
  static const secondary700 = Color(0xFF0369A1);

  static const gray50 = Color(0xFFF9FAFB);
  static const gray100 = Color(0xFFF3F4F6);
  static const gray200 = Color(0xFFE5E7EB);
  static const gray300 = Color(0xFFD1D5DB);
  static const gray400 = Color(0xFF9CA3AF);
  static const gray500 = Color(0xFF6B7280);
  static const gray600 = Color(0xFF4B5563);
  static const gray700 = Color(0xFF374151);
  static const gray900 = Color(0xFF111827);
}

/// Altura mínima dos alvos de toque. O motorista usa de pé, com uma mão, às vezes
/// de luva — 48dp é o piso das diretrizes de acessibilidade, não um exagero aqui.
const double kTouchTarget = 48;

ThemeData buildAppTheme() {
  final base = ThemeData(
    useMaterial3: true,
    colorScheme: ColorScheme.fromSeed(
      seedColor: AppColors.primary600,
      primary: AppColors.primary600,
      error: AppColors.danger600,
    ),
    scaffoldBackgroundColor: AppColors.gray50,
  );

  return base.copyWith(
    appBarTheme: const AppBarTheme(
      backgroundColor: AppColors.primary600,
      foregroundColor: Colors.white,
      elevation: 0,
      centerTitle: false,
      titleTextStyle: TextStyle(
        color: Colors.white,
        fontSize: 20,
        fontWeight: FontWeight.w800,
      ),
    ),
    cardTheme: CardThemeData(
      color: Colors.white,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: const BorderSide(color: AppColors.gray200),
      ),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size.fromHeight(52),
        backgroundColor: AppColors.primary600,
        foregroundColor: Colors.white,
        textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w800),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        minimumSize: const Size.fromHeight(52),
        foregroundColor: AppColors.gray700,
        side: const BorderSide(color: AppColors.gray200, width: 1.5),
        textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: Colors.white,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: AppColors.gray200, width: 1.5),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: AppColors.gray200, width: 1.5),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: AppColors.primary500, width: 1.8),
      ),
    ),
    dividerTheme: const DividerThemeData(color: AppColors.gray200, space: 1, thickness: 1),
  );
}
