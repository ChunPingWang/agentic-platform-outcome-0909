namespace LifePremium.Domain.Services;

using LifePremium.Domain.Entities;

public interface ICalculationRecordRepository
{
    void Save(CalculationRecord record);
    IEnumerable<CalculationRecord> GetByAgentId(string agentId);
}
