# StudyQuiz — Future Roadmap
_Discussed with Anandu on 2026-09-28. Implement only when he asks._

## ✅ Implemented in v1.3 (2026-09-28)
1. **Double-back to exit** — single back press on Home shows bilingual toast
   ("Press back again to exit / പുറത്തുകടക്കാൻ വീണ്ടും അമർത്തൂ"); second
   press within 2s exits.
2. **3-tab bottom nav: Home | Profile | Settings** — theme (Light/Dark/System),
   language toggle, backup/restore and guest-name change moved out of Profile
   into the new Settings tab. Profile = My Analysis only + avatar change.
3. **Avatar picker** — 12 emoji animal avatars on coloured circles
   (🦁 🐯 🐘 🐼 🦊 🐰 🐒 🐸 🦉 🐧 🐬 🦄), picked at profile creation,
   changeable from Profile tab / Settings. Replaces the alphabet initial.
4. **Mock test mode** — Home → "Mock Test": 100 questions (or all available),
   90-minute countdown, 1 mark each; OMR-style palette grid
   (answered/unanswered/current, tap to jump); auto-submit on timer end;
   result screen with score %.
5. **Bookmarks** — ☆/⭐ toggle on every question (quiz + mock test);
   Home → "Bookmarks" entry lists starred questions; tap to revise one,
   or "Revise all".
6. **Wrong-answer revision** — wrong question IDs tracked in Store; Home →
   "തെറ്റിയത് മാത്രം / Wrong only (N)" quizzes only those; a question drops
   out once answered correctly.
7. **Exam countdown** — Settings → exam date (DatePicker); Home shows
   "CSEB Exam — N days to go / N ദിവസം ബാക്കി" card (prompts to set if unset).
8. **Daily study reminder** — Settings → toggle + time picker (default 9 PM);
   AlarmManager + BroadcastReceiver, bilingual notification
   ("പഠിക്കാൻ സമയമായി! 📚 / Time to study!"); POST_NOTIFICATIONS runtime
   permission on Android 13+; rescheduled on reboot.
9. **Feedback nudge on exit** — on double-back exit, ~1-in-3 chance, max once
   per day, shows a dialog offering the anonymous feedback option.
10. **Credit in Settings** — "Made by Anandu / അനന്ദു നിർമ്മിച്ചത്" +
    tappable "@an_an_dhu.sreeni141" → https://instagram.com/an_an_dhu.sreeni141.
- Bookmarks + wrong IDs persist in studyquiz.json (included in backup/restore).

## ✅ Implemented in v1.1 (2026-09-28)
1. **Guest profile** — name asked on first launch, stored locally.
   "Hi, {name}" greeting + profile icon (top-right) with name initial.
2. **Feedback (NOT via direct message)** — anonymous feedback button in
   profile. Wired to `Config.FEEDBACK_FORM_URL` (empty until Anandu
   creates a Google Form and shares the link).
3. **Export / Import backup** — via Storage Access Framework (no permission
   needed). Backup: profile → Backup → pick location. Restore: pick file.
4. **Anonymous install counter** — one ping on first launch, wired to
   `Config.COUNTER_URL` (empty until Apps Script endpoint is set up —
   needs Anandu's Google account).
5. **App icon** — custom icon (book + checkmark + growth chart, teal).
6. **Profile icon (top-right) + My Analysis screen** — stats, growth
   line-chart, per-topic breakdown (true per-question attribution via
   perTopic in Attempt), study streak, history (long-press delete).
   Old ProgressActivity merged into ProfileActivity.
7. **"?" Help/About icon** — bilingual (ML+EN) help: aim, how-to-use,
   privacy note ("no data leaves the phone"), backup/restore steps.
8. **Bilingual UI** — full app in മലയാളം + English with in-app toggle
   (profile → Language). Help content always shows both languages.
9. **Theme: Light / Dark / System** — in-app switcher (profile → Theme);
   activity base theme switches too so dialogs follow.

## Planned features (in no particular order)
_(none pending — all discussed items are in v1.1)_
1. **Guest profile** — ask name on first launch, store locally (no login/server).
   Show "Hi, <name>" in app; attach name to quiz results/progress.
2. **Feedback (NOT via direct message — Anandu's preference 2026-09-28)**
   Options:
   a. **Google Form link** — "Feedback" button opens a Google Form; responses
      land in his Sheet anonymously. Zero code, free.
   b. **In-app anonymous rating** — 1–5 stars + optional comment, sent
      anonymously to his Sheet via the same Apps Script infra as the
      install counter (roadmap item 4). No names, no direct messages.
3. **Export / Import backup** — export all data (topics, questions, attempts)
   to a JSON file; import it back after reinstall. User-controlled, offline.
4. **Anonymous install counter** — one ping on first launch to a free counter
   (Apps Script → Google Sheet), so Anandu can see how many people installed.
   No login, no names, just a count. Must disclose in-app (privacy note).
5. **App icon** — custom launcher icon (currently default).
6. **Profile icon (top-right) + My Analysis screen**
   - Header row: title left, circular profile icon right (guest initial).
   - Profile screen shows personal data analysis:
     - Guest name + stats: total quizzes, average %, best %
     - Growth line-chart (already exists in Progress — reuse/move here)
     - **Per-topic breakdown**: average % per topic → "strong/weak topics"
       (most useful for studying: *where to focus*)
     - **Study streak**: consecutive days with ≥1 quiz — motivating
     - Total questions answered, overall accuracy
   - This effectively merges/extends the current Progress screen into the profile.
7. **"?" Help/About icon** (top area, next to profile icon)
   - Tapping shows a dialog/screen with two sections:
     - **നമ്മുടെ ഉദ്ദേശം (Our aim)**: why the app was made — e.g.
       "പഠനം രസകരവും trackable-ഉം ആക്കുക — സ്വന്തം notes-ഉം
       PYQ-കളും ചേർത്ത് quiz ചെയ്ത് ഓരോ ദിവസവും growth കാണുക."
       (Anandu to finalize wording in his own words.)
     - **എങ്ങനെ ഉപയോഗിക്കാം (How to use)**: numbered steps —
       1) "+ Add Topic" → പേരും notes-ഉം, 2) "+ Add MCQ" → ചോദ്യങ്ങൾ,
       3) "Start Quiz" → 10/20/All തിരഞ്ഞെടുത്ത് quiz,
       4) "My Progress" → score, growth chart, history.
     - **🔒 Privacy + Backup note** (Anandu's draft idea 2026-09-28):
       "ഈ app നിന്റെ ഒരു വിവരവും എവിടെയും save ചെയ്യുന്നില്ല —
       ഇത് ആരാണ് ഉപയോഗിക്കുന്നതെന്ന് developer പോലും അറിയില്ല.
       നിന്റെ topics, questions, scores എല്ലാം നിന്റെ phone-ൽ
       മാത്രമാണ് ഇരിക്കുന്നത്. അതുകൊണ്ട് app uninstall ചെയ്താൽ
       data പോകും — ഇടയ്ക്ക് backup എടുക്കൂ."
       + Backup/restore steps:
       Backup: Home → "Backup" → file save ചെയ്ത് WhatsApp-ൽ
         സ്വന്തത്തിന് അയക്കൂ / Drive-ൽ വെക്കൂ.
       Restore: Reinstall-ന് ശേഷം "Restore" → ആ file തിരഞ്ഞെടുക്കൂ.
8. **Bilingual: മലയാളം + English**
   - Phase 1 (first): "?" Help/About content in both languages —
     Malayalam version + English version stacked, or a small
     മല/EN toggle inside the help screen.
   - Phase 2 (later): full app UI bilingual — every button/label/message
     in both languages, with an in-app language switch in settings
     (simpler & more predictable than following phone language).
   - Note: Phase 2 is real work — every user-visible string × 2.
9. **Theme: Light / Dark / System** — in-app theme switcher (in profile).
   Programmatic UI goes fully theme-aware; activity base theme also
   switches so dialogs follow.

## Decided AGAINST (for now)
- Google / phone-number login — overkill; needs Firebase/server, SMS costs,
  privacy policy. Guest profile covers 90% of the need.
- Play Store publishing — $25 + 12 testers/14 days closed testing; direct APK
  sharing via WhatsApp is enough for now.
- Cloud sync — overkill at this stage.

## Notes
- `android:allowBackup="true"` already set — Google Auto Backup may restore
  data on reinstall in some cases, but export/import is the reliable path.
- If analytics ever needed properly: Firebase Analytics (free), needs Firebase
  project setup under Anandu's Google account.

## Planned (Anandu's list — implement only when he says go)
1. **Double-back to exit** (2026-09-28): single back press on Home must NOT close
   the app — require two back presses within ~2s ("Press back again to exit"
   toast, bilingual). Applies to MainActivity home screen.
2. **3-tab bottom nav: Home | Profile | Settings** (2026-09-28): new Settings tab;
   move theme (Light/Dark/System), language toggle, backup/restore (and guest
   name change) out of Profile into Settings. Profile keeps My Analysis only.
   (Confirmed 2026-09-28: Profile tab = analysis ONLY — stats, growth chart,
   per-topic breakdown, streak, history. No settings/controls there.)
3. **Overall colour theme change** (2026-09-28): Anandu wants the app's colour
   theme changed; he will share the new colours/design later. Do nothing yet.
4. **Read-only app for users** (2026-09-28): remove Add Topic / Add MCQ / edit /
   delete content options — users can only read notes, take quizzes, see
   analysis. All content comes bundled by Anandu via new APK versions.
5. **Feedback nudge on app exit** (2026-09-28): when closing the app, show a
   message asking users to send feedback (e.g. request new notes/topics) via
   the feedback option. Show only occasionally (Anandu: "idakk eppozhelum"),
   e.g. at most once a day / once every few closes — never on every exit.
6. **App credit in Settings** (2026-09-28): show "Made by Anandu" (with Malayalam
   അനന്ദു) credit line in the Settings tab.
   + Instagram link: @an_an_dhu.sreeni141 (tappable — opens Instagram;
   "fans vallathum varuvanenkil varatte" 😌).

## Content plan (v1.3)
- Target exam: Kerala CSEB (Co-operative Service Examination Board) — co-operative
  bank exams (confirmed 2026-09-28).
- Flow: Anandu sends topic list → Julie drafts notes + MCQs from web research →
  Anandu verifies accuracy → bundled into app as built-in read-only content.
- He may also send his own notes as PDF (topic-wise).
7. **Avatar picker** (2026-09-28): replace alphabet initial with selectable animal
   cartoon avatars (emoji-based, no asset files needed — drawn on coloured
   circle). User picks one at profile creation; changeable later in Profile.
   Candidates: lion, tiger, elephant, panda, fox, rabbit, monkey, frog, owl,
   penguin, dolphin, unicorn. Suggested 2026-09-28; implement only when he says go.
8. **Mock test mode** (APPROVED 2026-09-28): real-exam simulation — 100 questions,
   90 minutes timer, 100 marks. OMR-style flow.
9. **Bookmark questions** (APPROVED 2026-09-28): star difficult questions, revise
   later from a bookmarks list.
10. **Wrong-answer revision** (APPROVED 2026-09-28): quiz mode with only
    previously-wrong questions ("തെറ്റിയത് മാത്രം").
11. **Exam countdown** (APPROVED 2026-09-28): days remaining to next CSEB exam on
    Home screen.
12. **Daily study reminder** (APPROVED 2026-09-28): daily notification reminder.

## Content scope (2026-09-28)
User narrowed initial StudyQuiz content to TWO exams only:
- Junior Co-operative Inspector (Kerala PSC)
- Assistant Secretary (CSEB — paper 1044/2023 already in verification PDF)
Junior Co-op Inspector PYQs not yet collected; awaiting his go-ahead to research.
- 2026-09-28 update: user says CSEB **Junior Clerk / Cashier is the MAIN post**.
  3 Junior Clerk papers already in verification PDF: 1045/2023 (10/2023,
  13-01-2024), 1990/2021 (5/2021, 14-11-2021), 1982/2021 (2/2021, 30-07-2021).
- 2026-09-28: LD Clerk EXCLUDED from scope (user: "Ld clerk venda").

## ✅ Implemented in v1.5 (2026-09-29) — read-only mode
- **Built-in content infra**: `assets/builtin_content.json` (`{topics: [{id, name, notes, questions: [{q, options[4], answer, explanation}]}]}`), loaded at startup by `BuiltinContent.kt` (org.json, no new deps). Question model gained `explanation` (shown as 💡 in topic detail).
- **Read-only UI**: removed + Add Topic, + Add MCQ, Edit/Delete topic & question buttons everywhere; deleted EditTopicActivity/EditQuestionActivity (+ manifest entries). Users can read notes, quiz (normal/mock/bookmarks/wrong-only), analysis, upcoming exams, settings.
- **User data untouched**: attempts, bookmarks, wrong-ids, upcoming exams, reminder, avatar, name all keep working against built-in topics. Backup/restore now exports user data only; import accepts old backups (content keys ignored).
- Ships with empty `topics: []` — friendly "Content coming soon / ഉള്ളടക്കം ഉടൻ വരും" empty state, no crashes. Content flow: Anandu sends content → bundled into `builtin_content.json` → new APK release.
- Help "?" text rewritten for read-only (no add-topic/add-MCQ steps).

## ✅ Implemented in v1.7 (2026-09-29) — notifications, share, backend wiring
- **PSC 090/2026 PDF bundled**: `assets/notifications/090-2026.pdf` (6 pages, verified Category 90/2026); `upcoming_exams.json` psc-090-2026.pdf now set. All 11 notification PDFs bundled.
- **Credit change**: Settings → About now shows "Developer ©️" (was "Made by Anandu / അനന്ദു നിർമ്മിച്ചത്"); tappable @an_an_dhu.sreeni141 kept below.
- **Notification bell**: 🔔 button at top-right of Ui.topBar (all screens); opens new NotificationsActivity listing `assets/app_notifications.json` items (newest first, bilingual). Unread badge: "🔔N" count of items whose id not in SharedPreferences seen-set; opening the screen marks all seen. Seeded with 2 items (2026-09-29): exams update + welcome.
- **Share button**: Settings → "📤 Share app / ആപ്പ് ഷെയർ ചെയ്യൂ" — ACTION_SEND text with GitHub releases link; bilingual chooser title.
- **Backend wired**: Config.COUNTER_URL (Apps Script, verified incrementing) and Config.FEEDBACK_FORM_URL (Google Form, public) set in Lang.kt. Existing pingCounterOnce()/openFeedback() now live.

## ✅ Implemented in v1.9 (2026-09-29) — year-wise PYQ folders
- **Two-level PYQ navigation** (Anandu's refinement): `PyqPapersActivity` is now the **year-folder list** (📁 2026, newest first, "N papers / N പേപ്പറുകൾ" per year; blank-year papers under "📁 Other / മറ്റുള്ളവ" at the end). Tapping a year opens new `PyqYearActivity` with that year's papers (category number prominent 22sp teal bold + title + question count); tapping a paper opens the unchanged `PyqExamActivity` flow.
- **PyqData.papersByYear()**: groups by `year` field, null-safe; empty state unchanged. pyq.json still empty — framework only, awaiting his PYQ PDFs.

## ✅ Implemented in v1.10 (2026-09-29) — PYQ exam UX + figure support
- **1045/2023 paper bundled**: `assets/pyq.json` now has the Junior Clerk 1045/2023 paper (Alpha Code C, 153 questions; 7 official-key-cancelled excluded; answers from the final answer key).
- **Prev/Next navigation** (Anandu): `PyqExamActivity` has "◀ Previous / മുമ്പത്തെ" and "Next ▶ / അടുത്തത് ▶" below the options. Option-tap still records + auto-advances; Previous goes back (pick kept, changeable, highlighted); Next skips without answering (counts as unanswered); on the last question Next becomes "Finish ✓ / പൂർത്തിയാക്കൂ ✓". Picks tracked in a MutableMap (question→option), converted to IntArray with -1 = unanswered for the result.
- **Live timer**: elapsed ticker (MM:SS, H:MM:SS past an hour) at top-right of the exam header, Handler-updated every second, removed in onDestroy. Elapsed ms passed to `PyqResultActivity` via `elapsed_ms` extra.
- **Richer result**: score card shows ✅ N right / ❌ N wrong / ⏭ N unanswered ("⏭ %d ഉത്തരം നൽകാത്തവ") + percentage + "⏱ Time: 12m 34s" / "⏱ എടുത്ത സമയം: 12 മിനിറ്റ് 34 സെക്കൻഡ്" (zero units dropped, bilingual).
- **Figure image support**: `PyqQuestion.img` (optional asset path, null-safe); Q43 ("triangles in the given figure") tagged `"img":"pyq_img/q43.png"`; new `Ui.assetImage()` helper (fit-width, maxHeight 300dp, null on decode failure); image shown between question text and options in exam + in the review list.

## ✅ Implemented in v1.11 (2026-09-29) — 1044/2023 paper + arrow nav + welcome popup
- **1044/2023 paper bundled**: `assets/pyq.json` now has 2 papers — Junior Clerk 1045/2023 (153 Qs) + NEW Assistant Secretary 1044/2023 (Alpha Code D, 153 Qs; 7 official-key-cancelled excluded: 39,47,50,93,94,95,99; answers from the final answer key, stored as 0–3 indices like 1045/2023).
- **Figure questions as images** (Anandu: no text descriptions): 5 figure questions in 1044/2023 ship with cropped original diagrams — Q48/Q57/Q49 single question-image (`img`: q48.png, q57.png, q49.png); Q44 Venn-diagram options + Q45 answer-figure options via new `optImgs` ([4] asset paths); Q45 also shows problem-figures strip (`img`: q45prob.png). 13 PNGs total in `assets/pyq_img/` (q43 + 12 new).
- **Per-option image support**: `PyqQuestion.optImgs` (null-safe parse); new `Ui.optionRow()` helper (button-styled row: "A." label + asset image, filled = selected); used in `PyqExamActivity` and the result review list. Q49's blank question text hides the question TextView (image IS the question, no empty gap).
- **Bottom-fixed arrow nav bar** (Anandu's explicit request, replaces v1.10 text buttons): ◀ / ▶ big arrows fixed at the bottom (don't scroll), Previous dimmed/disabled on Q1, ▶ becomes ✓ Finish on the last question. Option-tap auto-advance, pick retained + highlighted, live timer, ✅/❌/⏭ + time result all kept.
- **First-install welcome popup** (Anandu): English-only AlertDialog on very first launch — title "🎉 Welcome 🎉", body explains the app + how PYQ Papers/quizzes/analysis work + offline note, button "Continue". Shown once (SharedPreferences `welcome_shown` + session guard), never again.
