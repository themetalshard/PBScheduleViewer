import SwiftUI

struct ScheduleView: View {
    @StateObject private var vm = ScheduleViewModel()
    @AppStorage("is_dark_mode") private var isDarkMode = true

    var body: some View {
        NavigationStack {
            Group {
                if vm.isLoading && vm.schedule.isEmpty {
                    ProgressView("Fetching Schedules...")
                } else {
                    let events = vm.schedule[vm.activeGroup] ?? []
                    
                    if events.isEmpty {
                        ContentUnavailableView("No schedules for \(vm.activeGroup.uppercased())", systemImage: "calendar.badge.exclamationmark")
                    } else if vm.isCalendarView {
                        HorizontalCalendarView(events: events, vm: vm)
                    } else {
                        List(events) { event in
                            EventRow(event: event)
                                .contentShape(Rectangle())
                                .onTapGesture { vm.selectedEvent = event }
                        }
                        .listStyle(.plain)
                        .refreshable { await vm.refresh() }
                    }
                }
            }
            .navigationTitle(vm.activeGroup.uppercased())
            .sheet(item: $vm.selectedEvent) { event in
                EventDetailView(event: event)
            }
            .toolbar {
                ToolbarItem(placement: .bottomBar) {
                    HStack {
                        NavButton(id: "pbst", icon: "shield.fill", vm: vm)
                        NavButton(id: "pet", icon: "cross.case.fill", vm: vm)
                        NavButton(id: "tms", icon: "flame.fill", vm: vm)
                        NavButton(id: "pbm", icon: "camera.fill", vm: vm)
                    }
                }
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button { vm.isCalendarView.toggle() } label: {
                        Image(systemName: vm.isCalendarView ? "list.bullet" : "calendar")
                    }
                }
            }
            .task { await vm.refresh() }
        }
        .preferredColorScheme(isDarkMode ? .dark : .light)
    }
}

struct EventDetailView: View {
    let event: ScheduleEvent
    @Environment(\.dismiss) var dismiss
    
    var utcDateString: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, h:mm a"
        formatter.timeZone = TimeZone(abbreviation: "UTC")
        return formatter.string(from: Date(timeIntervalSince1970: event.time)) + " UTC"
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(event.eventType)
                            .font(.title2).bold()
                            .foregroundColor(event.color)
                        
                        Text(event.notes ?? "No description.")
                            .font(.body)
                    }
                    .padding(.vertical, 8)
                }

                Section("Host Information") {
                    DetailRow(label: "Trainer Name", value: event.trainer ?? "N/A", icon: "person.fill", color: .blue)
                    DetailRow(label: "Trainer ID", value: "\(event.trainerId ?? 0)", icon: "number", color: .secondary)
                    DetailRow(label: "Discord ID", value: event.trainerCommsId ?? "N/A", icon: "bubble.left.and.bubble.right.fill", color: .indigo)
                }

                Section("Schedule") {
                    DetailRow(label: "Local Time", value: Date(timeIntervalSince1970: event.time).formatted(date: .abbreviated, time: .shortened), icon: "clock.badge.checkmark", color: .green)
                    
                    DetailRow(label: "UTC Time", value: utcDateString, icon: "globe", color: .purple)
                    
                    DetailRow(label: "Unix Timestamp", value: "\(Int(event.time))", icon: "cpu", color: .orange)
                    
                    DetailRow(label: "Duration", value: "\(event.duration) Minutes", icon: "timer", color: .red)
                }

                Section("API Identifiers") {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Training UUID").font(.caption).foregroundColor(.secondary)
                        Text(event.trainingID ?? "N/A")
                            .font(.system(.subheadline, design: .monospaced))
                            .textSelection(.enabled)
                    }
                }
            }
            .navigationTitle("Information")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}


struct EventRow: View {
    let event: ScheduleEvent
    var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 2).fill(event.color).frame(width: 4)
            VStack(alignment: .leading) {
                Text(Date(timeIntervalSince1970: event.time), style: .time)
                    .font(.caption).bold().foregroundColor(.blue)
                Text(event.eventType).font(.headline)
                Text("Host: \(event.trainer ?? "Unknown")").font(.subheadline).foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

struct HorizontalCalendarView: View {
    let events: [ScheduleEvent]
    @ObservedObject var vm: ScheduleViewModel
    
    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                ForEach(0..<events.count, id: \.self) { index in
                    let event = events[index]
                    
                    if index == 0 || !isSameDay(t1: event.time, t2: events[index-1].time) {
                        DateHeader(timestamp: event.time)
                    }
                    
                    HStack(alignment: .top, spacing: 15) {
                        VStack(alignment: .trailing) {
                            Text(Date(timeIntervalSince1970: event.time), style: .time)
                                .font(.system(.subheadline, design: .monospaced))
                                .bold()
                            
                            Rectangle()
                                .fill(Color.secondary.opacity(0.3))
                                .frame(width: 2)
                                .frame(maxHeight: .infinity)
                        }
                        .frame(width: 80)

                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(event.eventType).font(.headline)
                                Spacer()
                                Text("\(event.duration)m")
                                    .font(.caption2).bold()
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(Capsule().fill(event.color.opacity(0.2)))
                                    .foregroundColor(event.color)
                            }
                            
                            if let notes = event.notes, !notes.isEmpty {
                                Text(notes).font(.caption).foregroundColor(.secondary).lineLimit(3)
                            }
                            
                            Label(event.trainer ?? "Unknown", systemImage: "person.circle.fill")
                                .font(.caption).bold().foregroundColor(event.color)
                        }
                        .padding()
                        .background(RoundedRectangle(cornerRadius: 12).fill(Color(uiColor: .secondarySystemBackground)))
                        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(event.color.opacity(0.3), lineWidth: 1))
                        .onTapGesture { vm.selectedEvent = event }
                    }
                    .padding(.horizontal)
                }
            }
        }
    }

    private func isSameDay(t1: Double, t2: Double) -> Bool {
        let calendar = Calendar.current
        return calendar.isDate(Date(timeIntervalSince1970: t1), inSameDayAs: Date(timeIntervalSince1970: t2))
    }
}

struct DetailRow: View {
    let label: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(color)
                .font(.footnote)
                .frame(width: 20)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(.caption2).foregroundColor(.secondary).textCase(.uppercase)
                Text(value).font(.body).textSelection(.enabled)
            }
        }
        .padding(.vertical, 2)
    }
}

struct DateHeader: View {
    let timestamp: Double
    var body: some View {
        HStack {
            Text(Date(timeIntervalSince1970: timestamp).formatted(.dateTime.month().day()))
                .font(.title3).bold()
                .padding(.horizontal, 12)
                .padding(.vertical, 4)
                .background(Color.blue.opacity(0.1))
                .foregroundColor(.blue)
                .cornerRadius(8)
            Spacer()
            Rectangle().fill(Color.secondary.opacity(0.2)).frame(height: 1)
        }
        .padding(.horizontal)
        .padding(.top, 20)
        .padding(.bottom, 10)
    }
}

struct NavButton: View {
    let id: String
    let icon: String
    @ObservedObject var vm: ScheduleViewModel
    var body: some View {
        Button { vm.activeGroup = id } label: {
            VStack {
                Image(systemName: icon)
                Text(id.uppercased()).font(.caption2)
            }
            .foregroundColor(vm.activeGroup == id ? .blue : .secondary)
        }
        .frame(maxWidth: .infinity)
    }
}
