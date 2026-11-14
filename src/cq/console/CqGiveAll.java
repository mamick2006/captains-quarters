package cq.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import cq.campaign.CqDev;
import cq.campaign.CqStrings;
import cq.items.CqCatalog;

public class CqGiveAll implements BaseCommand {

  @Override
  public CommandResult runCommand(String args, CommandContext context) {
    if (!context.isInCampaign()) {
      Console.showMessage(CqStrings.console("console_wrong_context"));
      return CommandResult.WRONG_CONTEXT;
    }
    int added = CqDev.giveAllItems();
    int total = CqCatalog.all().size();
    if (added == 0) {
      Console.showMessage(String.format(CqStrings.console("console_nothing_to_add"), total));
    } else {
      Console.showMessage(String.format(CqStrings.console("console_added"), added, total));
    }
    return CommandResult.SUCCESS;
  }
}
