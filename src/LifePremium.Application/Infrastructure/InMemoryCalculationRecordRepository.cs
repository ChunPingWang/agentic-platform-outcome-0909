namespace LifePremium.Application.Infrastructure;

using LifePremium.Domain.Entities;
using LifePremium.Domain.Services;

public sealed class InMemoryCalculationRecordRepository : ICalculationRecordRepository
{
    private readonly List<CalculationRecord> _records = [];

    public void Save(CalculationRecord record)
    {
        _records.Add(record);
    }

    public IEnumerable<CalculationRecord> GetByAgentId(string agentId)
    {
        return _records.Where(r => r.AgentId == agentId).ToList();
    }

    public IEnumerable<CalculationRecord> GetAll() => _records.ToList();
}
