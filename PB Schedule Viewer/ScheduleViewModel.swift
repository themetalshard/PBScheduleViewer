import Foundation
import Combine

import SwiftUI

@MainActor
class ScheduleViewModel: ObservableObject {
    @Published var schedule: [String: [ScheduleEvent]] = [:]
    @Published var isLoading = false
    @Published var activeGroup = "pbst"
    @Published var isCalendarView = false
    @Published var selectedEvent: ScheduleEvent?
    
    
    private let repository = ScheduleRepository()

    func refresh() async {
        isLoading = true
        do {
            self.schedule = try await repository.fetchSchedule()
        } catch {
            print("Error: \(error)")
        }
        isLoading = false
    }
}
