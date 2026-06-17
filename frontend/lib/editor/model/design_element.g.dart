// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'design_element.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$TextElementImpl _$$TextElementImplFromJson(Map<String, dynamic> json) =>
    _$TextElementImpl(
      id: json['id'] as String,
      x: (json['x'] as num).toDouble(),
      y: (json['y'] as num).toDouble(),
      width: (json['width'] as num).toDouble(),
      height: (json['height'] as num).toDouble(),
      rotation: (json['rotation'] as num?)?.toDouble() ?? 0,
      opacity: (json['opacity'] as num?)?.toDouble() ?? 1,
      zIndex: (json['zIndex'] as num?)?.toInt() ?? 0,
      locked: json['locked'] as bool? ?? false,
      text: json['text'] as String,
      fontSize: (json['fontSize'] as num?)?.toDouble() ?? 48,
      fontFamily: json['fontFamily'] as String? ?? 'Roboto',
      color: json['color'] as String? ?? '#FFFFFFFF',
      align: json['align'] as String? ?? 'left',
      weight: (json['weight'] as num?)?.toInt() ?? 400,
      $type: json['type'] as String?,
    );

Map<String, dynamic> _$$TextElementImplToJson(_$TextElementImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'x': instance.x,
      'y': instance.y,
      'width': instance.width,
      'height': instance.height,
      'rotation': instance.rotation,
      'opacity': instance.opacity,
      'zIndex': instance.zIndex,
      'locked': instance.locked,
      'text': instance.text,
      'fontSize': instance.fontSize,
      'fontFamily': instance.fontFamily,
      'color': instance.color,
      'align': instance.align,
      'weight': instance.weight,
      'type': instance.$type,
    };

_$ImageElementImpl _$$ImageElementImplFromJson(Map<String, dynamic> json) =>
    _$ImageElementImpl(
      id: json['id'] as String,
      x: (json['x'] as num).toDouble(),
      y: (json['y'] as num).toDouble(),
      width: (json['width'] as num).toDouble(),
      height: (json['height'] as num).toDouble(),
      rotation: (json['rotation'] as num?)?.toDouble() ?? 0,
      opacity: (json['opacity'] as num?)?.toDouble() ?? 1,
      zIndex: (json['zIndex'] as num?)?.toInt() ?? 0,
      locked: json['locked'] as bool? ?? false,
      assetId: json['assetId'] as String,
      fit: json['fit'] as String? ?? 'contain',
      $type: json['type'] as String?,
    );

Map<String, dynamic> _$$ImageElementImplToJson(_$ImageElementImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'x': instance.x,
      'y': instance.y,
      'width': instance.width,
      'height': instance.height,
      'rotation': instance.rotation,
      'opacity': instance.opacity,
      'zIndex': instance.zIndex,
      'locked': instance.locked,
      'assetId': instance.assetId,
      'fit': instance.fit,
      'type': instance.$type,
    };

_$ShapeElementImpl _$$ShapeElementImplFromJson(Map<String, dynamic> json) =>
    _$ShapeElementImpl(
      id: json['id'] as String,
      x: (json['x'] as num).toDouble(),
      y: (json['y'] as num).toDouble(),
      width: (json['width'] as num).toDouble(),
      height: (json['height'] as num).toDouble(),
      rotation: (json['rotation'] as num?)?.toDouble() ?? 0,
      opacity: (json['opacity'] as num?)?.toDouble() ?? 1,
      zIndex: (json['zIndex'] as num?)?.toInt() ?? 0,
      locked: json['locked'] as bool? ?? false,
      shape: json['shape'] as String? ?? 'rect',
      fill: json['fill'] as String? ?? '#FF888888',
      stroke: json['stroke'] as String? ?? '#00000000',
      cornerRadius: (json['cornerRadius'] as num?)?.toDouble() ?? 0,
      $type: json['type'] as String?,
    );

Map<String, dynamic> _$$ShapeElementImplToJson(_$ShapeElementImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'x': instance.x,
      'y': instance.y,
      'width': instance.width,
      'height': instance.height,
      'rotation': instance.rotation,
      'opacity': instance.opacity,
      'zIndex': instance.zIndex,
      'locked': instance.locked,
      'shape': instance.shape,
      'fill': instance.fill,
      'stroke': instance.stroke,
      'cornerRadius': instance.cornerRadius,
      'type': instance.$type,
    };

_$VideoElementImpl _$$VideoElementImplFromJson(Map<String, dynamic> json) =>
    _$VideoElementImpl(
      id: json['id'] as String,
      x: (json['x'] as num).toDouble(),
      y: (json['y'] as num).toDouble(),
      width: (json['width'] as num).toDouble(),
      height: (json['height'] as num).toDouble(),
      rotation: (json['rotation'] as num?)?.toDouble() ?? 0,
      opacity: (json['opacity'] as num?)?.toDouble() ?? 1,
      zIndex: (json['zIndex'] as num?)?.toInt() ?? 0,
      locked: json['locked'] as bool? ?? false,
      assetId: json['assetId'] as String,
      autoplay: json['autoplay'] as bool? ?? false,
      loop: json['loop'] as bool? ?? true,
      $type: json['type'] as String?,
    );

Map<String, dynamic> _$$VideoElementImplToJson(_$VideoElementImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'x': instance.x,
      'y': instance.y,
      'width': instance.width,
      'height': instance.height,
      'rotation': instance.rotation,
      'opacity': instance.opacity,
      'zIndex': instance.zIndex,
      'locked': instance.locked,
      'assetId': instance.assetId,
      'autoplay': instance.autoplay,
      'loop': instance.loop,
      'type': instance.$type,
    };
