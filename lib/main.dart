import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:intl/intl.dart';
import 'event_model.dart';

void main() => runApp(const PBScheduleApp());

class PBScheduleApp extends StatelessWidget {
  const PBScheduleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'PB Schedule Viewer',
      theme: ThemeData.dark(useMaterial3: true).copyWith(
        scaffoldBackgroundColor: const Color(0xFF1A1A1A),
        cardTheme: const CardThemeData(color: Color(0xFF252525)),
      ),
      home: const ScheduleHomePage(),
    );
  }
}

class ScheduleHomePage extends StatefulWidget {
  const ScheduleHomePage({super.key});

  @override
  State<ScheduleHomePage> createState() => _ScheduleHomePageState();
}

class _ScheduleHomePageState extends State<ScheduleHomePage> {
  Map<String, List<ScheduleEvent>> schedule = {};
  String activeGroup = "pbst";
  bool isLoading = true;
  bool isCalendarView = true;

  final ScrollController _horizontalController = ScrollController();

  @override
  void dispose() {
    _horizontalController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    fetchData();
  }

  Future<void> fetchData() async {
    try {
      final response = await http.get(Uri.parse('https://pbsv.themetalshard.space/schedule.json'));
      if (response.statusCode == 200) {
        final Map<String, dynamic> data = json.decode(response.body);
        Map<String, List<ScheduleEvent>> tempSchedule = {};

        data.forEach((group, eventsMap) {
          if (eventsMap is Map<String, dynamic>) {
            List<ScheduleEvent> events = [];
            eventsMap.forEach((uuid, eventData) {
              events.add(ScheduleEvent.fromJson(uuid, eventData));
            });
            events.sort((a, b) => a.time.compareTo(b.time));
            tempSchedule[group] = events;
          }
        });

        setState(() {
          schedule = tempSchedule;
          isLoading = false;
        });
      }
    } catch (e) {
      debugPrint("Error: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("PB Schedule Viewer"),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(50),
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                const SizedBox(width: 10),
                ...schedule.keys.map((group) => Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4),
                  child: ChoiceChip(
                    label: Text(group.toUpperCase()),
                    selected: activeGroup == group,
                    onSelected: (val) => setState(() => activeGroup = group),
                  ),
                )),
                const SizedBox(width: 20),
                DropdownButton<bool>(
                  value: isCalendarView,
                  underline: const SizedBox(),
                  items: const [
                    DropdownMenuItem(value: false, child: Text("List")),
                    DropdownMenuItem(value: true, child: Text("Calendar")),
                  ],
                  onChanged: (val) => setState(() => isCalendarView = val!),
                ),
                const SizedBox(width: 10),
              ],
            ),
          ),
        ),
      ),
      body: isLoading 
          ? const Center(child: CircularProgressIndicator()) 
          : (isCalendarView ? buildCalendarView() : buildListView()),
    );
  }

  Widget buildListView() {
    final events = schedule[activeGroup] ?? [];
    return ListView.builder(
      itemCount: events.length,
      itemBuilder: (context, i) => EventCard(event: events[i]),
    );
  }

  Widget buildCalendarView() {
    final events = schedule[activeGroup] ?? [];
    Map<String, List<ScheduleEvent>> grouped = {};
    
    for (var e in events) {
      String date = DateFormat('EEE dd/MM').format(DateTime.fromMillisecondsSinceEpoch(e.time * 1000));
      grouped.putIfAbsent(date, () => []).add(e);
    }

    return Scrollbar(
      controller: _horizontalController, 
      thumbVisibility: true,
      child: SingleChildScrollView(
        controller: _horizontalController,
        scrollDirection: Axis.horizontal,
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: grouped.entries.map((entry) {
            final verticalController = ScrollController();
            
            return SizedBox(
              width: 280,
              child: Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.all(12.0),
                    child: Text(entry.key, style: Theme.of(context).textTheme.titleLarge),
                  ),
                  const Divider(height: 1),
                  Expanded(
                    child: Scrollbar(
                      controller: verticalController, // Assign vertical controller
                      child: ListView.builder(
                        controller: verticalController, // Use same controller here
                        primary: false, // Prevents conflicts
                        padding: const EdgeInsets.only(bottom: 20),
                        itemCount: entry.value.length,
                        itemBuilder: (context, index) {
                          return EventCard(event: entry.value[index]);
                        },
                      ),
                    ),
                  ),
                ],
              ),
            );
          }).toList(),
        ),
      ),
    );
  }
}

class EventCard extends StatelessWidget {
  final ScheduleEvent event;
  const EventCard({super.key, required this.event});

  @override
  Widget build(BuildContext context) {
    final startTime = DateTime.fromMillisecondsSinceEpoch(event.time * 1000);
    final endTime = startTime.add(Duration(minutes: event.duration));
    final color = event.eventColor != null 
        ? Color.fromARGB(255, event.eventColor![0], event.eventColor![1], event.eventColor![2])
        : Colors.grey;

    return GestureDetector(
      onTap: () => showDialog(context: context, builder: (c) => EventDetailsDialog(event: event)),
      child: Card(
        margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        child: IntrinsicHeight(
          child: Row(
            children: [
              Container(width: 6, color: color),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.all(12.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text("${DateFormat('HH:mm').format(startTime)} - ${DateFormat('HH:mm').format(endTime)}", 
                           style: TextStyle(color: Colors.grey[400], fontSize: 12)),
                      const SizedBox(height: 4),
                      Text(event.eventType, 
                           style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                      if (event.trainer != null) 
                        Text("Host: ${event.trainer}", style: const TextStyle(fontSize: 13)),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class EventDetailsDialog extends StatelessWidget {
  final ScheduleEvent event;
  const EventDetailsDialog({super.key, required this.event});

  @override
  Widget build(BuildContext context) {
    final local = DateTime.fromMillisecondsSinceEpoch(event.time * 1000);
    final utc = local.toUtc();

    return Dialog(
      backgroundColor: const Color(0xFF222228),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      child: Container(
        constraints: const BoxConstraints(maxWidth: 500),
        padding: const EdgeInsets.fromLTRB(24, 24, 24, 16),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                event.eventType,
                style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white),
              ),
              const SizedBox(height: 20),
              
              _infoRow("Local start:", DateFormat('yyyy-MM-dd HH:mm:ss').format(local)),
              _infoRow("UTC start:", DateFormat('yyyy-MM-dd HH:mm:ss').format(utc)),
              _infoRow("Unix timestamp:", event.time.toString()),
              _infoRow("Duration:", "${event.duration} minutes"),
              
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: Divider(color: Colors.white24, height: 1),
              ),
              
              if (event.trainer != null) _infoRow("Host:", event.trainer!),
              
              if (event.notes != null) ...[
                const SizedBox(height: 8),
                Text(
                  "Notes: ${event.notes}",
                  style: TextStyle(color: Colors.grey[300], height: 1.4),
                ),
              ],
              
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: Divider(color: Colors.white24, height: 1),
              ),
              
              _infoRow("UUID:", event.uuid ?? "N/A", isSmall: true),
              if (event.trainerId != null) 
                _infoRow("Trainer ID:", event.trainerId.toString(), isSmall: true),
              if (event.discordId != null) 
                _infoRow("Discord ID:", event.discordId!, isSmall: true),
              
              const SizedBox(height: 12),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: () => Navigator.pop(context),
                  style: TextButton.styleFrom(foregroundColor: const Color(0xFFD1C4E9)),
                  child: const Text("Close", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _infoRow(String label, String value, {bool isSmall = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: RichText(
        text: TextSpan(
          style: TextStyle(
            fontSize: isSmall ? 12 : 14,
            color: isSmall ? Colors.grey[500] : Colors.grey[300],
          ),
          children: [
            TextSpan(text: "$label ", style: const TextStyle(fontWeight: FontWeight.w500)),
            TextSpan(text: value),
          ],
        ),
      ),
    );
  }
}