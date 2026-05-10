import Foundation

class ScheduleRepository {
    private let url = URL(string: "https://pbsv.themetalshard.space/api/")!

    func fetchSchedule() async throws -> [String: [ScheduleEvent]] {
        let (data, _) = try await URLSession.shared.data(from: url)
        
        guard let jsonArray = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            return [:]
        }
        
        var finalMap: [String: [ScheduleEvent]] = [:]
        let decoder = JSONDecoder()

        for groupMap in jsonArray {
            guard let groupId = groupMap["id"] as? String else { continue }
            
            var eventsInGroup: [ScheduleEvent] = []
            
            for (key, value) in groupMap where key != "id" {
                if let eventDict = value as? [String: Any] {
                    do {
                        let eventData = try JSONSerialization.data(withJSONObject: eventDict)
                        let event = try decoder.decode(ScheduleEvent.self, from: eventData)
                        eventsInGroup.append(event)
                    } catch {
                        print("Failed to decode event \(key): \(error)")
                    }
                }
            }
            
            finalMap[groupId] = eventsInGroup.sorted(by: { $0.time < $1.time })
        }
        
        return finalMap
    }
}
