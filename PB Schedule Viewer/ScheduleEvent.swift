import Foundation
import SwiftUI

struct ScheduleEvent: Codable, Identifiable {
    let id = UUID()
    
    let eventType: String
    let time: Double
    let duration: Int
    let trainer: String?
    let trainerId: Int64?
    let trainingID: String?
    let trainerCommsId: String?
    let notes: String?
    let eventColor: [Int]?

    enum CodingKeys: String, CodingKey {
        case eventType = "EventType"
        case time = "Time"
        case duration = "Duration"
        case trainer = "Trainer"
        case trainerId = "TrainerId"
        case trainingID = "TrainingID"
        case trainerCommsId = "TrainerCommsId"
        case notes = "Notes"
        case eventColor = "EventColor"
    }

    var color: Color {
        guard let rgb = eventColor, rgb.count == 3 else { return .gray }
        return Color(
            red: Double(rgb[0]) / 255.0,
            green: Double(rgb[1]) / 255.0,
            blue: Double(rgb[2]) / 255.0
        )
    }
}
