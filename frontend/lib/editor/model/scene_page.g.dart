// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'scene_page.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$ScenePageImpl _$$ScenePageImplFromJson(Map<String, dynamic> json) =>
    _$ScenePageImpl(
      id: json['id'] as String,
      elements: (json['elements'] as List<dynamic>?)
              ?.map((e) => DesignElement.fromJson(e as Map<String, dynamic>))
              .toList() ??
          const <DesignElement>[],
    );

Map<String, dynamic> _$$ScenePageImplToJson(_$ScenePageImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'elements': instance.elements,
    };
