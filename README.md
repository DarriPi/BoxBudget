# BoxBudget

A minimal Android budgeting app. Give each spending "box" a percentage of your salary, log what you buy, and see at a glance what's left. A box turns red when you overspend it.

All data stays on your phone. The app has no account, needs no internet connection and asks for no permissions.

## Features

- **Boxes:** create a box with a name, an icon or emoji, and a share of your salary. Its rand budget is worked out for you.
- **Amount left:** the header shows what's left of your salary, and it updates with every purchase.
- **Items:** add an item and price to a box. Items are saved, and you can edit a name or price or remove the item later.
- **Over-budget warning:** a box turns red and shows how much you overspent.
- **Monthly reset:** reset by hand, or automatically on your payday. Each reset saves the month to History first.
- **CSV export:** save your past months as a `.csv` file for Excel, Google Sheets or similar tools.
- **Quick tutorial:** a short walkthrough shows new users how the app works. It appears only the first time the app opens.

## Install

You need an Android phone running Android 8.0 or newer.

### With Android Studio

1. Clone this repository and open the folder in Android Studio.
2. On your phone, turn on **Developer options** and **USB debugging**, then connect it with a USB cable.
3. Accept the **Allow USB debugging?** prompt on the phone.
4. Select your phone in the device list and press **Run**.

### From the command line

With the Android SDK installed and your phone connected:

```bash
git clone https://github.com/DarriPi/BoxBudget.git
```

```bash
cd BoxBudget
```

```bash
./gradlew installDebug
```

On Windows PowerShell, use `.\gradlew.bat installDebug` for the last step.

## How to use

1. **Set your salary.** On the Boxes screen, tap the pencil next to the salary.
2. **Add a box.** Tap **Add box**, name it, pick an icon or emoji, and drag the slider to set its share of your salary. Boxes can't add up to more than 100%.
3. **Log a purchase.** Tap a box, enter the item and price, and tap **Add to [box]**.
4. **Change or remove an item.** Tap the item in the **This month** list, then choose **Save** or **Remove**.
5. **Change or delete a box.** Open the box and tap the pencil at the top right. **Delete box** is at the bottom of that screen.

### Reading the Boxes screen

| Label | Meaning |
| --- | --- |
| **Monthly salary** | The salary you entered |
| **left this month** | Salary minus everything you've bought so far |
| **Spent from boxes** | Total spent, out of the total budget of all boxes |
| **Not in a box yet** | The part of your salary no box has claimed |

### Monthly reset

Open the **History** tab.

- **Reset every payday:** tick the box to reset automatically. Tap the text to choose the day of the month. If a month is too short for that day, the reset happens on its last day.
- **Reset now and save this month:** closes the month straight away. A manual reset doesn't change the next automatic date.

A reset saves the month to **Past months** and empties every box. Your boxes and salary stay as they are.

The automatic reset runs when you open the app on or after payday, so it can't happen while the app stays closed. If you miss several paydays, your spending is saved as one month.

With automatic reset on, a period is named after the month it ends in. With a payday on the 25th, 25 October to 24 November is "November".

### Export to CSV

On the **History** tab, tap **Export CSV** and choose where to save the file. The button is available once at least one month has been saved.

The file has one row per item, with these columns:

`month, closed_on, salary, box, box_percent, box_budget, box_spent, box_over_budget, item, item_date, item_price`

## Development

Built with Kotlin and Jetpack Compose (Material 3).

| Path | Contents |
| --- | --- |
| `app/src/main/java/com/example/boxbudget/data` | Models, budget maths, reset rules, CSV and JSON storage |
| `app/src/main/java/com/example/boxbudget/ui` | Screens and the view model |

Run the unit tests with:

```bash
./gradlew testDebugUnitTest
```
