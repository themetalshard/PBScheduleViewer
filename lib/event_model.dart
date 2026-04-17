class ScheduleEvent {
  final int time;
  final int duration;
  final String eventType;
  final String? trainer;
  final String? notes;
  final int? trainerId;
  final String? discordId;
  final String? uuid;
  final List<int>? eventColor;

  ScheduleEvent({
    required this.time,
    required this.duration,
    required this.eventType,
    this.trainer,
    this.notes,
    this.trainerId,
    this.discordId,
    this.uuid,
    this.eventColor,
  });

  factory ScheduleEvent.fromJson(String id, Map<String, dynamic> json) {
    return ScheduleEvent(
      time: json['Time'] ?? 0,
      duration: json['Duration'] ?? 0,
      eventType: json['EventType'] ?? 'Unknown',
      trainer: json['Trainer'],
      notes: json['Notes'],
      trainerId: json['TrainerId'],
      discordId: json['TrainerCommsId'],
      uuid: json['TrainingID'] ?? id,
      eventColor: json['EventColor'] != null ? List<int>.from(json['EventColor']) : null,
    );
  }
}