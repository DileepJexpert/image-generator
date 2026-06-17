// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'design_element.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

DesignElement _$DesignElementFromJson(Map<String, dynamic> json) {
  switch (json['type']) {
    case 'text':
      return TextElement.fromJson(json);
    case 'image':
      return ImageElement.fromJson(json);
    case 'shape':
      return ShapeElement.fromJson(json);
    case 'video':
      return VideoElement.fromJson(json);

    default:
      throw CheckedFromJsonException(json, 'type', 'DesignElement',
          'Invalid union type "${json['type']}"!');
  }
}

/// @nodoc
mixin _$DesignElement {
  String get id => throw _privateConstructorUsedError;
  double get x => throw _privateConstructorUsedError;
  double get y => throw _privateConstructorUsedError;
  double get width => throw _privateConstructorUsedError;
  double get height => throw _privateConstructorUsedError;
  double get rotation => throw _privateConstructorUsedError;
  double get opacity => throw _privateConstructorUsedError;
  int get zIndex => throw _privateConstructorUsedError;
  bool get locked => throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)
        text,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)
        image,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)
        shape,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)
        video,
  }) =>
      throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)?
        text,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)?
        image,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)?
        shape,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)?
        video,
  }) =>
      throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)?
        text,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)?
        image,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)?
        shape,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)?
        video,
    required TResult orElse(),
  }) =>
      throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(TextElement value) text,
    required TResult Function(ImageElement value) image,
    required TResult Function(ShapeElement value) shape,
    required TResult Function(VideoElement value) video,
  }) =>
      throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(TextElement value)? text,
    TResult? Function(ImageElement value)? image,
    TResult? Function(ShapeElement value)? shape,
    TResult? Function(VideoElement value)? video,
  }) =>
      throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(TextElement value)? text,
    TResult Function(ImageElement value)? image,
    TResult Function(ShapeElement value)? shape,
    TResult Function(VideoElement value)? video,
    required TResult orElse(),
  }) =>
      throw _privateConstructorUsedError;

  /// Serializes this DesignElement to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $DesignElementCopyWith<DesignElement> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DesignElementCopyWith<$Res> {
  factory $DesignElementCopyWith(
          DesignElement value, $Res Function(DesignElement) then) =
      _$DesignElementCopyWithImpl<$Res, DesignElement>;
  @useResult
  $Res call(
      {String id,
      double x,
      double y,
      double width,
      double height,
      double rotation,
      double opacity,
      int zIndex,
      bool locked});
}

/// @nodoc
class _$DesignElementCopyWithImpl<$Res, $Val extends DesignElement>
    implements $DesignElementCopyWith<$Res> {
  _$DesignElementCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? x = null,
    Object? y = null,
    Object? width = null,
    Object? height = null,
    Object? rotation = null,
    Object? opacity = null,
    Object? zIndex = null,
    Object? locked = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      x: null == x
          ? _value.x
          : x // ignore: cast_nullable_to_non_nullable
              as double,
      y: null == y
          ? _value.y
          : y // ignore: cast_nullable_to_non_nullable
              as double,
      width: null == width
          ? _value.width
          : width // ignore: cast_nullable_to_non_nullable
              as double,
      height: null == height
          ? _value.height
          : height // ignore: cast_nullable_to_non_nullable
              as double,
      rotation: null == rotation
          ? _value.rotation
          : rotation // ignore: cast_nullable_to_non_nullable
              as double,
      opacity: null == opacity
          ? _value.opacity
          : opacity // ignore: cast_nullable_to_non_nullable
              as double,
      zIndex: null == zIndex
          ? _value.zIndex
          : zIndex // ignore: cast_nullable_to_non_nullable
              as int,
      locked: null == locked
          ? _value.locked
          : locked // ignore: cast_nullable_to_non_nullable
              as bool,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TextElementImplCopyWith<$Res>
    implements $DesignElementCopyWith<$Res> {
  factory _$$TextElementImplCopyWith(
          _$TextElementImpl value, $Res Function(_$TextElementImpl) then) =
      __$$TextElementImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      double x,
      double y,
      double width,
      double height,
      double rotation,
      double opacity,
      int zIndex,
      bool locked,
      String text,
      double fontSize,
      String fontFamily,
      String color,
      String align,
      int weight});
}

/// @nodoc
class __$$TextElementImplCopyWithImpl<$Res>
    extends _$DesignElementCopyWithImpl<$Res, _$TextElementImpl>
    implements _$$TextElementImplCopyWith<$Res> {
  __$$TextElementImplCopyWithImpl(
      _$TextElementImpl _value, $Res Function(_$TextElementImpl) _then)
      : super(_value, _then);

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? x = null,
    Object? y = null,
    Object? width = null,
    Object? height = null,
    Object? rotation = null,
    Object? opacity = null,
    Object? zIndex = null,
    Object? locked = null,
    Object? text = null,
    Object? fontSize = null,
    Object? fontFamily = null,
    Object? color = null,
    Object? align = null,
    Object? weight = null,
  }) {
    return _then(_$TextElementImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      x: null == x
          ? _value.x
          : x // ignore: cast_nullable_to_non_nullable
              as double,
      y: null == y
          ? _value.y
          : y // ignore: cast_nullable_to_non_nullable
              as double,
      width: null == width
          ? _value.width
          : width // ignore: cast_nullable_to_non_nullable
              as double,
      height: null == height
          ? _value.height
          : height // ignore: cast_nullable_to_non_nullable
              as double,
      rotation: null == rotation
          ? _value.rotation
          : rotation // ignore: cast_nullable_to_non_nullable
              as double,
      opacity: null == opacity
          ? _value.opacity
          : opacity // ignore: cast_nullable_to_non_nullable
              as double,
      zIndex: null == zIndex
          ? _value.zIndex
          : zIndex // ignore: cast_nullable_to_non_nullable
              as int,
      locked: null == locked
          ? _value.locked
          : locked // ignore: cast_nullable_to_non_nullable
              as bool,
      text: null == text
          ? _value.text
          : text // ignore: cast_nullable_to_non_nullable
              as String,
      fontSize: null == fontSize
          ? _value.fontSize
          : fontSize // ignore: cast_nullable_to_non_nullable
              as double,
      fontFamily: null == fontFamily
          ? _value.fontFamily
          : fontFamily // ignore: cast_nullable_to_non_nullable
              as String,
      color: null == color
          ? _value.color
          : color // ignore: cast_nullable_to_non_nullable
              as String,
      align: null == align
          ? _value.align
          : align // ignore: cast_nullable_to_non_nullable
              as String,
      weight: null == weight
          ? _value.weight
          : weight // ignore: cast_nullable_to_non_nullable
              as int,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$TextElementImpl extends TextElement {
  const _$TextElementImpl(
      {required this.id,
      required this.x,
      required this.y,
      required this.width,
      required this.height,
      this.rotation = 0,
      this.opacity = 1,
      this.zIndex = 0,
      this.locked = false,
      required this.text,
      this.fontSize = 48,
      this.fontFamily = 'Roboto',
      this.color = '#FFFFFFFF',
      this.align = 'left',
      this.weight = 400,
      final String? $type})
      : $type = $type ?? 'text',
        super._();

  factory _$TextElementImpl.fromJson(Map<String, dynamic> json) =>
      _$$TextElementImplFromJson(json);

  @override
  final String id;
  @override
  final double x;
  @override
  final double y;
  @override
  final double width;
  @override
  final double height;
  @override
  @JsonKey()
  final double rotation;
  @override
  @JsonKey()
  final double opacity;
  @override
  @JsonKey()
  final int zIndex;
  @override
  @JsonKey()
  final bool locked;
  @override
  final String text;
  @override
  @JsonKey()
  final double fontSize;
  @override
  @JsonKey()
  final String fontFamily;
  @override
  @JsonKey()
  final String color;
  @override
  @JsonKey()
  final String align;
  @override
  @JsonKey()
  final int weight;

  @JsonKey(name: 'type')
  final String $type;

  @override
  String toString() {
    return 'DesignElement.text(id: $id, x: $x, y: $y, width: $width, height: $height, rotation: $rotation, opacity: $opacity, zIndex: $zIndex, locked: $locked, text: $text, fontSize: $fontSize, fontFamily: $fontFamily, color: $color, align: $align, weight: $weight)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TextElementImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.x, x) || other.x == x) &&
            (identical(other.y, y) || other.y == y) &&
            (identical(other.width, width) || other.width == width) &&
            (identical(other.height, height) || other.height == height) &&
            (identical(other.rotation, rotation) ||
                other.rotation == rotation) &&
            (identical(other.opacity, opacity) || other.opacity == opacity) &&
            (identical(other.zIndex, zIndex) || other.zIndex == zIndex) &&
            (identical(other.locked, locked) || other.locked == locked) &&
            (identical(other.text, text) || other.text == text) &&
            (identical(other.fontSize, fontSize) ||
                other.fontSize == fontSize) &&
            (identical(other.fontFamily, fontFamily) ||
                other.fontFamily == fontFamily) &&
            (identical(other.color, color) || other.color == color) &&
            (identical(other.align, align) || other.align == align) &&
            (identical(other.weight, weight) || other.weight == weight));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      x,
      y,
      width,
      height,
      rotation,
      opacity,
      zIndex,
      locked,
      text,
      fontSize,
      fontFamily,
      color,
      align,
      weight);

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$TextElementImplCopyWith<_$TextElementImpl> get copyWith =>
      __$$TextElementImplCopyWithImpl<_$TextElementImpl>(this, _$identity);

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)
        text,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)
        image,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)
        shape,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)
        video,
  }) {
    return text(id, x, y, width, height, rotation, opacity, zIndex, locked,
        this.text, fontSize, fontFamily, color, align, weight);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)?
        text,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)?
        image,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)?
        shape,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)?
        video,
  }) {
    return text?.call(id, x, y, width, height, rotation, opacity, zIndex,
        locked, this.text, fontSize, fontFamily, color, align, weight);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)?
        text,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)?
        image,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)?
        shape,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)?
        video,
    required TResult orElse(),
  }) {
    if (text != null) {
      return text(id, x, y, width, height, rotation, opacity, zIndex, locked,
          this.text, fontSize, fontFamily, color, align, weight);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(TextElement value) text,
    required TResult Function(ImageElement value) image,
    required TResult Function(ShapeElement value) shape,
    required TResult Function(VideoElement value) video,
  }) {
    return text(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(TextElement value)? text,
    TResult? Function(ImageElement value)? image,
    TResult? Function(ShapeElement value)? shape,
    TResult? Function(VideoElement value)? video,
  }) {
    return text?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(TextElement value)? text,
    TResult Function(ImageElement value)? image,
    TResult Function(ShapeElement value)? shape,
    TResult Function(VideoElement value)? video,
    required TResult orElse(),
  }) {
    if (text != null) {
      return text(this);
    }
    return orElse();
  }

  @override
  Map<String, dynamic> toJson() {
    return _$$TextElementImplToJson(
      this,
    );
  }
}

abstract class TextElement extends DesignElement {
  const factory TextElement(
      {required final String id,
      required final double x,
      required final double y,
      required final double width,
      required final double height,
      final double rotation,
      final double opacity,
      final int zIndex,
      final bool locked,
      required final String text,
      final double fontSize,
      final String fontFamily,
      final String color,
      final String align,
      final int weight}) = _$TextElementImpl;
  const TextElement._() : super._();

  factory TextElement.fromJson(Map<String, dynamic> json) =
      _$TextElementImpl.fromJson;

  @override
  String get id;
  @override
  double get x;
  @override
  double get y;
  @override
  double get width;
  @override
  double get height;
  @override
  double get rotation;
  @override
  double get opacity;
  @override
  int get zIndex;
  @override
  bool get locked;
  String get text;
  double get fontSize;
  String get fontFamily;
  String get color;
  String get align;
  int get weight;

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$TextElementImplCopyWith<_$TextElementImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class _$$ImageElementImplCopyWith<$Res>
    implements $DesignElementCopyWith<$Res> {
  factory _$$ImageElementImplCopyWith(
          _$ImageElementImpl value, $Res Function(_$ImageElementImpl) then) =
      __$$ImageElementImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      double x,
      double y,
      double width,
      double height,
      double rotation,
      double opacity,
      int zIndex,
      bool locked,
      String assetId,
      String fit});
}

/// @nodoc
class __$$ImageElementImplCopyWithImpl<$Res>
    extends _$DesignElementCopyWithImpl<$Res, _$ImageElementImpl>
    implements _$$ImageElementImplCopyWith<$Res> {
  __$$ImageElementImplCopyWithImpl(
      _$ImageElementImpl _value, $Res Function(_$ImageElementImpl) _then)
      : super(_value, _then);

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? x = null,
    Object? y = null,
    Object? width = null,
    Object? height = null,
    Object? rotation = null,
    Object? opacity = null,
    Object? zIndex = null,
    Object? locked = null,
    Object? assetId = null,
    Object? fit = null,
  }) {
    return _then(_$ImageElementImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      x: null == x
          ? _value.x
          : x // ignore: cast_nullable_to_non_nullable
              as double,
      y: null == y
          ? _value.y
          : y // ignore: cast_nullable_to_non_nullable
              as double,
      width: null == width
          ? _value.width
          : width // ignore: cast_nullable_to_non_nullable
              as double,
      height: null == height
          ? _value.height
          : height // ignore: cast_nullable_to_non_nullable
              as double,
      rotation: null == rotation
          ? _value.rotation
          : rotation // ignore: cast_nullable_to_non_nullable
              as double,
      opacity: null == opacity
          ? _value.opacity
          : opacity // ignore: cast_nullable_to_non_nullable
              as double,
      zIndex: null == zIndex
          ? _value.zIndex
          : zIndex // ignore: cast_nullable_to_non_nullable
              as int,
      locked: null == locked
          ? _value.locked
          : locked // ignore: cast_nullable_to_non_nullable
              as bool,
      assetId: null == assetId
          ? _value.assetId
          : assetId // ignore: cast_nullable_to_non_nullable
              as String,
      fit: null == fit
          ? _value.fit
          : fit // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$ImageElementImpl extends ImageElement {
  const _$ImageElementImpl(
      {required this.id,
      required this.x,
      required this.y,
      required this.width,
      required this.height,
      this.rotation = 0,
      this.opacity = 1,
      this.zIndex = 0,
      this.locked = false,
      required this.assetId,
      this.fit = 'contain',
      final String? $type})
      : $type = $type ?? 'image',
        super._();

  factory _$ImageElementImpl.fromJson(Map<String, dynamic> json) =>
      _$$ImageElementImplFromJson(json);

  @override
  final String id;
  @override
  final double x;
  @override
  final double y;
  @override
  final double width;
  @override
  final double height;
  @override
  @JsonKey()
  final double rotation;
  @override
  @JsonKey()
  final double opacity;
  @override
  @JsonKey()
  final int zIndex;
  @override
  @JsonKey()
  final bool locked;
  @override
  final String assetId;
  @override
  @JsonKey()
  final String fit;

  @JsonKey(name: 'type')
  final String $type;

  @override
  String toString() {
    return 'DesignElement.image(id: $id, x: $x, y: $y, width: $width, height: $height, rotation: $rotation, opacity: $opacity, zIndex: $zIndex, locked: $locked, assetId: $assetId, fit: $fit)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$ImageElementImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.x, x) || other.x == x) &&
            (identical(other.y, y) || other.y == y) &&
            (identical(other.width, width) || other.width == width) &&
            (identical(other.height, height) || other.height == height) &&
            (identical(other.rotation, rotation) ||
                other.rotation == rotation) &&
            (identical(other.opacity, opacity) || other.opacity == opacity) &&
            (identical(other.zIndex, zIndex) || other.zIndex == zIndex) &&
            (identical(other.locked, locked) || other.locked == locked) &&
            (identical(other.assetId, assetId) || other.assetId == assetId) &&
            (identical(other.fit, fit) || other.fit == fit));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, x, y, width, height,
      rotation, opacity, zIndex, locked, assetId, fit);

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$ImageElementImplCopyWith<_$ImageElementImpl> get copyWith =>
      __$$ImageElementImplCopyWithImpl<_$ImageElementImpl>(this, _$identity);

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)
        text,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)
        image,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)
        shape,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)
        video,
  }) {
    return image(id, x, y, width, height, rotation, opacity, zIndex, locked,
        assetId, fit);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)?
        text,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)?
        image,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)?
        shape,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)?
        video,
  }) {
    return image?.call(id, x, y, width, height, rotation, opacity, zIndex,
        locked, assetId, fit);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)?
        text,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)?
        image,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)?
        shape,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)?
        video,
    required TResult orElse(),
  }) {
    if (image != null) {
      return image(id, x, y, width, height, rotation, opacity, zIndex, locked,
          assetId, fit);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(TextElement value) text,
    required TResult Function(ImageElement value) image,
    required TResult Function(ShapeElement value) shape,
    required TResult Function(VideoElement value) video,
  }) {
    return image(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(TextElement value)? text,
    TResult? Function(ImageElement value)? image,
    TResult? Function(ShapeElement value)? shape,
    TResult? Function(VideoElement value)? video,
  }) {
    return image?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(TextElement value)? text,
    TResult Function(ImageElement value)? image,
    TResult Function(ShapeElement value)? shape,
    TResult Function(VideoElement value)? video,
    required TResult orElse(),
  }) {
    if (image != null) {
      return image(this);
    }
    return orElse();
  }

  @override
  Map<String, dynamic> toJson() {
    return _$$ImageElementImplToJson(
      this,
    );
  }
}

abstract class ImageElement extends DesignElement {
  const factory ImageElement(
      {required final String id,
      required final double x,
      required final double y,
      required final double width,
      required final double height,
      final double rotation,
      final double opacity,
      final int zIndex,
      final bool locked,
      required final String assetId,
      final String fit}) = _$ImageElementImpl;
  const ImageElement._() : super._();

  factory ImageElement.fromJson(Map<String, dynamic> json) =
      _$ImageElementImpl.fromJson;

  @override
  String get id;
  @override
  double get x;
  @override
  double get y;
  @override
  double get width;
  @override
  double get height;
  @override
  double get rotation;
  @override
  double get opacity;
  @override
  int get zIndex;
  @override
  bool get locked;
  String get assetId;
  String get fit;

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$ImageElementImplCopyWith<_$ImageElementImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class _$$ShapeElementImplCopyWith<$Res>
    implements $DesignElementCopyWith<$Res> {
  factory _$$ShapeElementImplCopyWith(
          _$ShapeElementImpl value, $Res Function(_$ShapeElementImpl) then) =
      __$$ShapeElementImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      double x,
      double y,
      double width,
      double height,
      double rotation,
      double opacity,
      int zIndex,
      bool locked,
      String shape,
      String fill,
      String stroke,
      double cornerRadius});
}

/// @nodoc
class __$$ShapeElementImplCopyWithImpl<$Res>
    extends _$DesignElementCopyWithImpl<$Res, _$ShapeElementImpl>
    implements _$$ShapeElementImplCopyWith<$Res> {
  __$$ShapeElementImplCopyWithImpl(
      _$ShapeElementImpl _value, $Res Function(_$ShapeElementImpl) _then)
      : super(_value, _then);

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? x = null,
    Object? y = null,
    Object? width = null,
    Object? height = null,
    Object? rotation = null,
    Object? opacity = null,
    Object? zIndex = null,
    Object? locked = null,
    Object? shape = null,
    Object? fill = null,
    Object? stroke = null,
    Object? cornerRadius = null,
  }) {
    return _then(_$ShapeElementImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      x: null == x
          ? _value.x
          : x // ignore: cast_nullable_to_non_nullable
              as double,
      y: null == y
          ? _value.y
          : y // ignore: cast_nullable_to_non_nullable
              as double,
      width: null == width
          ? _value.width
          : width // ignore: cast_nullable_to_non_nullable
              as double,
      height: null == height
          ? _value.height
          : height // ignore: cast_nullable_to_non_nullable
              as double,
      rotation: null == rotation
          ? _value.rotation
          : rotation // ignore: cast_nullable_to_non_nullable
              as double,
      opacity: null == opacity
          ? _value.opacity
          : opacity // ignore: cast_nullable_to_non_nullable
              as double,
      zIndex: null == zIndex
          ? _value.zIndex
          : zIndex // ignore: cast_nullable_to_non_nullable
              as int,
      locked: null == locked
          ? _value.locked
          : locked // ignore: cast_nullable_to_non_nullable
              as bool,
      shape: null == shape
          ? _value.shape
          : shape // ignore: cast_nullable_to_non_nullable
              as String,
      fill: null == fill
          ? _value.fill
          : fill // ignore: cast_nullable_to_non_nullable
              as String,
      stroke: null == stroke
          ? _value.stroke
          : stroke // ignore: cast_nullable_to_non_nullable
              as String,
      cornerRadius: null == cornerRadius
          ? _value.cornerRadius
          : cornerRadius // ignore: cast_nullable_to_non_nullable
              as double,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$ShapeElementImpl extends ShapeElement {
  const _$ShapeElementImpl(
      {required this.id,
      required this.x,
      required this.y,
      required this.width,
      required this.height,
      this.rotation = 0,
      this.opacity = 1,
      this.zIndex = 0,
      this.locked = false,
      this.shape = 'rect',
      this.fill = '#FF888888',
      this.stroke = '#00000000',
      this.cornerRadius = 0,
      final String? $type})
      : $type = $type ?? 'shape',
        super._();

  factory _$ShapeElementImpl.fromJson(Map<String, dynamic> json) =>
      _$$ShapeElementImplFromJson(json);

  @override
  final String id;
  @override
  final double x;
  @override
  final double y;
  @override
  final double width;
  @override
  final double height;
  @override
  @JsonKey()
  final double rotation;
  @override
  @JsonKey()
  final double opacity;
  @override
  @JsonKey()
  final int zIndex;
  @override
  @JsonKey()
  final bool locked;
  @override
  @JsonKey()
  final String shape;
  @override
  @JsonKey()
  final String fill;
  @override
  @JsonKey()
  final String stroke;
  @override
  @JsonKey()
  final double cornerRadius;

  @JsonKey(name: 'type')
  final String $type;

  @override
  String toString() {
    return 'DesignElement.shape(id: $id, x: $x, y: $y, width: $width, height: $height, rotation: $rotation, opacity: $opacity, zIndex: $zIndex, locked: $locked, shape: $shape, fill: $fill, stroke: $stroke, cornerRadius: $cornerRadius)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$ShapeElementImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.x, x) || other.x == x) &&
            (identical(other.y, y) || other.y == y) &&
            (identical(other.width, width) || other.width == width) &&
            (identical(other.height, height) || other.height == height) &&
            (identical(other.rotation, rotation) ||
                other.rotation == rotation) &&
            (identical(other.opacity, opacity) || other.opacity == opacity) &&
            (identical(other.zIndex, zIndex) || other.zIndex == zIndex) &&
            (identical(other.locked, locked) || other.locked == locked) &&
            (identical(other.shape, shape) || other.shape == shape) &&
            (identical(other.fill, fill) || other.fill == fill) &&
            (identical(other.stroke, stroke) || other.stroke == stroke) &&
            (identical(other.cornerRadius, cornerRadius) ||
                other.cornerRadius == cornerRadius));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, x, y, width, height,
      rotation, opacity, zIndex, locked, shape, fill, stroke, cornerRadius);

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$ShapeElementImplCopyWith<_$ShapeElementImpl> get copyWith =>
      __$$ShapeElementImplCopyWithImpl<_$ShapeElementImpl>(this, _$identity);

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)
        text,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)
        image,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)
        shape,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)
        video,
  }) {
    return shape(id, x, y, width, height, rotation, opacity, zIndex, locked,
        this.shape, fill, stroke, cornerRadius);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)?
        text,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)?
        image,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)?
        shape,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)?
        video,
  }) {
    return shape?.call(id, x, y, width, height, rotation, opacity, zIndex,
        locked, this.shape, fill, stroke, cornerRadius);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)?
        text,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)?
        image,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)?
        shape,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)?
        video,
    required TResult orElse(),
  }) {
    if (shape != null) {
      return shape(id, x, y, width, height, rotation, opacity, zIndex, locked,
          this.shape, fill, stroke, cornerRadius);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(TextElement value) text,
    required TResult Function(ImageElement value) image,
    required TResult Function(ShapeElement value) shape,
    required TResult Function(VideoElement value) video,
  }) {
    return shape(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(TextElement value)? text,
    TResult? Function(ImageElement value)? image,
    TResult? Function(ShapeElement value)? shape,
    TResult? Function(VideoElement value)? video,
  }) {
    return shape?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(TextElement value)? text,
    TResult Function(ImageElement value)? image,
    TResult Function(ShapeElement value)? shape,
    TResult Function(VideoElement value)? video,
    required TResult orElse(),
  }) {
    if (shape != null) {
      return shape(this);
    }
    return orElse();
  }

  @override
  Map<String, dynamic> toJson() {
    return _$$ShapeElementImplToJson(
      this,
    );
  }
}

abstract class ShapeElement extends DesignElement {
  const factory ShapeElement(
      {required final String id,
      required final double x,
      required final double y,
      required final double width,
      required final double height,
      final double rotation,
      final double opacity,
      final int zIndex,
      final bool locked,
      final String shape,
      final String fill,
      final String stroke,
      final double cornerRadius}) = _$ShapeElementImpl;
  const ShapeElement._() : super._();

  factory ShapeElement.fromJson(Map<String, dynamic> json) =
      _$ShapeElementImpl.fromJson;

  @override
  String get id;
  @override
  double get x;
  @override
  double get y;
  @override
  double get width;
  @override
  double get height;
  @override
  double get rotation;
  @override
  double get opacity;
  @override
  int get zIndex;
  @override
  bool get locked;
  String get shape;
  String get fill;
  String get stroke;
  double get cornerRadius;

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$ShapeElementImplCopyWith<_$ShapeElementImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class _$$VideoElementImplCopyWith<$Res>
    implements $DesignElementCopyWith<$Res> {
  factory _$$VideoElementImplCopyWith(
          _$VideoElementImpl value, $Res Function(_$VideoElementImpl) then) =
      __$$VideoElementImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      double x,
      double y,
      double width,
      double height,
      double rotation,
      double opacity,
      int zIndex,
      bool locked,
      String assetId,
      bool autoplay,
      bool loop});
}

/// @nodoc
class __$$VideoElementImplCopyWithImpl<$Res>
    extends _$DesignElementCopyWithImpl<$Res, _$VideoElementImpl>
    implements _$$VideoElementImplCopyWith<$Res> {
  __$$VideoElementImplCopyWithImpl(
      _$VideoElementImpl _value, $Res Function(_$VideoElementImpl) _then)
      : super(_value, _then);

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? x = null,
    Object? y = null,
    Object? width = null,
    Object? height = null,
    Object? rotation = null,
    Object? opacity = null,
    Object? zIndex = null,
    Object? locked = null,
    Object? assetId = null,
    Object? autoplay = null,
    Object? loop = null,
  }) {
    return _then(_$VideoElementImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      x: null == x
          ? _value.x
          : x // ignore: cast_nullable_to_non_nullable
              as double,
      y: null == y
          ? _value.y
          : y // ignore: cast_nullable_to_non_nullable
              as double,
      width: null == width
          ? _value.width
          : width // ignore: cast_nullable_to_non_nullable
              as double,
      height: null == height
          ? _value.height
          : height // ignore: cast_nullable_to_non_nullable
              as double,
      rotation: null == rotation
          ? _value.rotation
          : rotation // ignore: cast_nullable_to_non_nullable
              as double,
      opacity: null == opacity
          ? _value.opacity
          : opacity // ignore: cast_nullable_to_non_nullable
              as double,
      zIndex: null == zIndex
          ? _value.zIndex
          : zIndex // ignore: cast_nullable_to_non_nullable
              as int,
      locked: null == locked
          ? _value.locked
          : locked // ignore: cast_nullable_to_non_nullable
              as bool,
      assetId: null == assetId
          ? _value.assetId
          : assetId // ignore: cast_nullable_to_non_nullable
              as String,
      autoplay: null == autoplay
          ? _value.autoplay
          : autoplay // ignore: cast_nullable_to_non_nullable
              as bool,
      loop: null == loop
          ? _value.loop
          : loop // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$VideoElementImpl extends VideoElement {
  const _$VideoElementImpl(
      {required this.id,
      required this.x,
      required this.y,
      required this.width,
      required this.height,
      this.rotation = 0,
      this.opacity = 1,
      this.zIndex = 0,
      this.locked = false,
      required this.assetId,
      this.autoplay = false,
      this.loop = true,
      final String? $type})
      : $type = $type ?? 'video',
        super._();

  factory _$VideoElementImpl.fromJson(Map<String, dynamic> json) =>
      _$$VideoElementImplFromJson(json);

  @override
  final String id;
  @override
  final double x;
  @override
  final double y;
  @override
  final double width;
  @override
  final double height;
  @override
  @JsonKey()
  final double rotation;
  @override
  @JsonKey()
  final double opacity;
  @override
  @JsonKey()
  final int zIndex;
  @override
  @JsonKey()
  final bool locked;
  @override
  final String assetId;
  @override
  @JsonKey()
  final bool autoplay;
  @override
  @JsonKey()
  final bool loop;

  @JsonKey(name: 'type')
  final String $type;

  @override
  String toString() {
    return 'DesignElement.video(id: $id, x: $x, y: $y, width: $width, height: $height, rotation: $rotation, opacity: $opacity, zIndex: $zIndex, locked: $locked, assetId: $assetId, autoplay: $autoplay, loop: $loop)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$VideoElementImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.x, x) || other.x == x) &&
            (identical(other.y, y) || other.y == y) &&
            (identical(other.width, width) || other.width == width) &&
            (identical(other.height, height) || other.height == height) &&
            (identical(other.rotation, rotation) ||
                other.rotation == rotation) &&
            (identical(other.opacity, opacity) || other.opacity == opacity) &&
            (identical(other.zIndex, zIndex) || other.zIndex == zIndex) &&
            (identical(other.locked, locked) || other.locked == locked) &&
            (identical(other.assetId, assetId) || other.assetId == assetId) &&
            (identical(other.autoplay, autoplay) ||
                other.autoplay == autoplay) &&
            (identical(other.loop, loop) || other.loop == loop));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, x, y, width, height,
      rotation, opacity, zIndex, locked, assetId, autoplay, loop);

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$VideoElementImplCopyWith<_$VideoElementImpl> get copyWith =>
      __$$VideoElementImplCopyWithImpl<_$VideoElementImpl>(this, _$identity);

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)
        text,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)
        image,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)
        shape,
    required TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)
        video,
  }) {
    return video(id, x, y, width, height, rotation, opacity, zIndex, locked,
        assetId, autoplay, loop);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)?
        text,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)?
        image,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)?
        shape,
    TResult? Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)?
        video,
  }) {
    return video?.call(id, x, y, width, height, rotation, opacity, zIndex,
        locked, assetId, autoplay, loop);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String text,
            double fontSize,
            String fontFamily,
            String color,
            String align,
            int weight)?
        text,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            String fit)?
        image,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String shape,
            String fill,
            String stroke,
            double cornerRadius)?
        shape,
    TResult Function(
            String id,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            double opacity,
            int zIndex,
            bool locked,
            String assetId,
            bool autoplay,
            bool loop)?
        video,
    required TResult orElse(),
  }) {
    if (video != null) {
      return video(id, x, y, width, height, rotation, opacity, zIndex, locked,
          assetId, autoplay, loop);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(TextElement value) text,
    required TResult Function(ImageElement value) image,
    required TResult Function(ShapeElement value) shape,
    required TResult Function(VideoElement value) video,
  }) {
    return video(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(TextElement value)? text,
    TResult? Function(ImageElement value)? image,
    TResult? Function(ShapeElement value)? shape,
    TResult? Function(VideoElement value)? video,
  }) {
    return video?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(TextElement value)? text,
    TResult Function(ImageElement value)? image,
    TResult Function(ShapeElement value)? shape,
    TResult Function(VideoElement value)? video,
    required TResult orElse(),
  }) {
    if (video != null) {
      return video(this);
    }
    return orElse();
  }

  @override
  Map<String, dynamic> toJson() {
    return _$$VideoElementImplToJson(
      this,
    );
  }
}

abstract class VideoElement extends DesignElement {
  const factory VideoElement(
      {required final String id,
      required final double x,
      required final double y,
      required final double width,
      required final double height,
      final double rotation,
      final double opacity,
      final int zIndex,
      final bool locked,
      required final String assetId,
      final bool autoplay,
      final bool loop}) = _$VideoElementImpl;
  const VideoElement._() : super._();

  factory VideoElement.fromJson(Map<String, dynamic> json) =
      _$VideoElementImpl.fromJson;

  @override
  String get id;
  @override
  double get x;
  @override
  double get y;
  @override
  double get width;
  @override
  double get height;
  @override
  double get rotation;
  @override
  double get opacity;
  @override
  int get zIndex;
  @override
  bool get locked;
  String get assetId;
  bool get autoplay;
  bool get loop;

  /// Create a copy of DesignElement
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$VideoElementImplCopyWith<_$VideoElementImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
