// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'project.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$ProjectImpl _$$ProjectImplFromJson(Map<String, dynamic> json) =>
    _$ProjectImpl(
      id: json['id'] as String,
      name: json['name'] as String,
      canvasWidth: (json['canvasWidth'] as num).toInt(),
      canvasHeight: (json['canvasHeight'] as num).toInt(),
      pages: (json['pages'] as List<dynamic>?)
              ?.map((e) => ScenePage.fromJson(e as Map<String, dynamic>))
              .toList() ??
          const <ScenePage>[],
    );

Map<String, dynamic> _$$ProjectImplToJson(_$ProjectImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'canvasWidth': instance.canvasWidth,
      'canvasHeight': instance.canvasHeight,
      'pages': instance.pages,
    };
