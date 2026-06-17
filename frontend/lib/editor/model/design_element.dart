import 'package:freezed_annotation/freezed_annotation.dart';

part 'design_element.freezed.dart';
part 'design_element.g.dart';

/// A single item on the design canvas. Sealed union over the element kinds
/// (CLAUDE.md section 7). The `type` discriminator keys the JSON, and the
/// common geometry fields (id/x/y/width/height/rotation/opacity/zIndex/locked)
/// are shared across every variant so they can be read and `copyWith`-updated
/// generically.
@Freezed(unionKey: 'type', unionValueCase: FreezedUnionCase.none)
class DesignElement with _$DesignElement {
  const DesignElement._();

  const factory DesignElement.text({
    required String id,
    required double x,
    required double y,
    required double width,
    required double height,
    @Default(0) double rotation,
    @Default(1) double opacity,
    @Default(0) int zIndex,
    @Default(false) bool locked,
    required String text,
    @Default(48) double fontSize,
    @Default('Roboto') String fontFamily,
    @Default('#FFFFFFFF') String color,
    @Default('left') String align,
    @Default(400) int weight,
  }) = TextElement;

  const factory DesignElement.image({
    required String id,
    required double x,
    required double y,
    required double width,
    required double height,
    @Default(0) double rotation,
    @Default(1) double opacity,
    @Default(0) int zIndex,
    @Default(false) bool locked,
    required String assetId,
    @Default('contain') String fit,
  }) = ImageElement;

  const factory DesignElement.shape({
    required String id,
    required double x,
    required double y,
    required double width,
    required double height,
    @Default(0) double rotation,
    @Default(1) double opacity,
    @Default(0) int zIndex,
    @Default(false) bool locked,
    @Default('rect') String shape,
    @Default('#FF888888') String fill,
    @Default('#00000000') String stroke,
    @Default(0) double cornerRadius,
  }) = ShapeElement;

  const factory DesignElement.video({
    required String id,
    required double x,
    required double y,
    required double width,
    required double height,
    @Default(0) double rotation,
    @Default(1) double opacity,
    @Default(0) int zIndex,
    @Default(false) bool locked,
    required String assetId,
    @Default(false) bool autoplay,
    @Default(true) bool loop,
  }) = VideoElement;

  factory DesignElement.fromJson(Map<String, dynamic> json) =>
      _$DesignElementFromJson(json);

  /// The asset id this element renders, if any (image/video).
  String? get assetIdOrNull => maybeMap(
        image: (e) => e.assetId,
        video: (e) => e.assetId,
        orElse: () => null,
      );
}
