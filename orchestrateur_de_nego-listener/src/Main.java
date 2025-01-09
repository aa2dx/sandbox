import Pricer.SpotPricer;
import QuoteSource.QuotePlatform;

public class Main {
    public static void main(String[] args) {

        System.out.println("Starting Application");

        var spotPricer = new SpotPricer();
        var workflowOrchestrator = new Orchestrator(spotPricer);
        var quotePlatform = new QuotePlatform(workflowOrchestrator);

        spotPricer.addListener(workflowOrchestrator);
        workflowOrchestrator.addListener(quotePlatform);

        spotPricer.start();
        workflowOrchestrator.start();
        quotePlatform.run();
        spotPricer.stop();
        workflowOrchestrator.stop();
    }
}