// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'scene_page.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

ScenePage _$ScenePageFromJson(Map<String, dynamic> json) {
  return _ScenePage.fromJson(json);
}

/// @nodoc
mixin _$ScenePage {
  String get id => throw _privateConstructorUsedError;
  List<DesignElement> get elements => throw _privateConstructorUsedError;

  /// Serializes this ScenePage to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of ScenePage
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $ScenePageCopyWith<ScenePage> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $ScenePageCopyWith<$Res> {
  factory $ScenePageCopyWith(ScenePage value, $Res Function(ScenePage) then) =
      _$ScenePageCopyWithImpl<$Res, ScenePage>;
  @useResult
  $Res call({String id, List<DesignElement> elements});
}

/// @nodoc
class _$ScenePageCopyWithImpl<$Res, $Val extends ScenePage>
    implements $ScenePageCopyWith<$Res> {
  _$ScenePageCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of ScenePage
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? elements = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      elements: null == elements
          ? _value.elements
          : elements // ignore: cast_nullable_to_non_nullable
              as List<DesignElement>,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$ScenePageImplCopyWith<$Res>
    implements $ScenePageCopyWith<$Res> {
  factory _$$ScenePageImplCopyWith(
          _$ScenePageImpl value, $Res Function(_$ScenePageImpl) then) =
      __$$ScenePageImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({String id, List<DesignElement> elements});
}

/// @nodoc
class __$$ScenePageImplCopyWithImpl<$Res>
    extends _$ScenePageCopyWithImpl<$Res, _$ScenePageImpl>
    implements _$$ScenePageImplCopyWith<$Res> {
  __$$ScenePageImplCopyWithImpl(
      _$ScenePageImpl _value, $Res Function(_$ScenePageImpl) _then)
      : super(_value, _then);

  /// Create a copy of ScenePage
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? elements = null,
  }) {
    return _then(_$ScenePageImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      elements: null == elements
          ? _value._elements
          : elements // ignore: cast_nullable_to_non_nullable
              as List<DesignElement>,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$ScenePageImpl extends _ScenePage {
  const _$ScenePageImpl(
      {required this.id,
      final List<DesignElement> elements = const <DesignElement>[]})
      : _elements = elements,
        super._();

  factory _$ScenePageImpl.fromJson(Map<String, dynamic> json) =>
      _$$ScenePageImplFromJson(json);

  @override
  final String id;
  final List<DesignElement> _elements;
  @override
  @JsonKey()
  List<DesignElement> get elements {
    if (_elements is EqualUnmodifiableListView) return _elements;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_elements);
  }

  @override
  String toString() {
    return 'ScenePage(id: $id, elements: $elements)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$ScenePageImpl &&
            (identical(other.id, id) || other.id == id) &&
            const DeepCollectionEquality().equals(other._elements, _elements));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType, id, const DeepCollectionEquality().hash(_elements));

  /// Create a copy of ScenePage
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$ScenePageImplCopyWith<_$ScenePageImpl> get copyWith =>
      __$$ScenePageImplCopyWithImpl<_$ScenePageImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$ScenePageImplToJson(
      this,
    );
  }
}

abstract class _ScenePage extends ScenePage {
  const factory _ScenePage(
      {required final String id,
      final List<DesignElement> elements}) = _$ScenePageImpl;
  const _ScenePage._() : super._();

  factory _ScenePage.fromJson(Map<String, dynamic> json) =
      _$ScenePageImpl.fromJson;

  @override
  String get id;
  @override
  List<DesignElement> get elements;

  /// Create a copy of ScenePage
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$ScenePageImplCopyWith<_$ScenePageImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
