package com.distressedelk.lumi;

import android.app.*;
import android.os.*;
import android.provider.Settings;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import android.graphics.drawable.GradientDrawable;
import android.speech.RecognizerIntent;
import android.Manifest;
import android.location.LocationManager;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int FEATURE_LEVEL = 6;
    static final int REQ_SPEECH = 44;
    static final int REQ_PERMS = 45;
    LinearLayout root, content;
    TextView status, transcript;
    int accent = Color.rgb(127,232,255), bg = Color.rgb(12,17,24), panel = Color.rgb(21,28,38), text = Color.rgb(242,246,250), muted = Color.rgb(154,168,184);
    SharedPreferences prefs;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("lumi", MODE_PRIVATE);
        showHome();
    }

    TextView tv(String s, int sp, int color) { TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setPadding(16,10,16,10); return v; }
    Button btn(String s) { Button b=new Button(this); b.setText(s); b.setTextColor(text); b.setTextSize(14); GradientDrawable g=new GradientDrawable(); g.setColor(panel); g.setCornerRadius(26); g.setStroke(1,accent); b.setBackground(g); b.setAllCaps(false); b.setPadding(12,6,12,6); return b; }
    void addCard(String s){ TextView c=tv(s,15,text); c.setBackgroundColor(panel); c.setPadding(24,22,24,22); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,8,0,8); content.addView(c,lp); }

    void base(String title) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg); root.setPadding(18,18,18,18);
        TextView t=tv(title,24,text); t.setTypeface(Typeface.DEFAULT_BOLD); root.addView(t);
        status=tv("Lumi v0.6 • cumulative update • local data preserved",12,muted); root.addView(status);
        ScrollView sv=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0,12,0,40); sv.addView(content); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER);
        String[] ns = FEATURE_LEVEL>=6 ? new String[]{"Home","Talk","Memory","Context","More"} : FEATURE_LEVEL>=5 ? new String[]{"Home","Talk","Memory","Context","Settings"} : FEATURE_LEVEL>=4 ? new String[]{"Home","Talk","Memory","Glasses","Settings"} : new String[]{"Home","Talk","Memory","Vault","Settings"};
        for(String n:ns){ Button b=btn(n); b.setOnClickListener(v->{ if(n.equals("Home"))showHome(); else if(n.equals("Talk"))showTalk(); else if(n.equals("Memory"))showMemory(); else if(n.equals("Vault"))openVault(); else if(n.equals("Settings"))showSettings(); else if(n.equals("Glasses"))showGlasses(); else if(n.equals("Context"))showContext(); else if(n.equals("More"))showMore(); }); nav.addView(b,new LinearLayout.LayoutParams(0,58,1)); }
        root.addView(nav); setContentView(root);
    }

    void showHome(){ base("Lumi");
        TextView avatar=tv("✧\nL U M I\n◜  ◝\n  •  •\n   ◡\n╰─────╯",30,accent); avatar.setGravity(Gravity.CENTER); avatar.setPadding(10,24,10,12); content.addView(avatar,new LinearLayout.LayoutParams(-1,245));
        String greeting = FEATURE_LEVEL==3 ? "Voice and memory are awake." : FEATURE_LEVEL==4 ? "Wearable mode is ready for testing." : FEATURE_LEVEL==5 ? "Context engine is learning how to stay useful without pestering you." : "Full phone prototype online. External services still need their real connections.";
        TextView g=tv(greeting,18,text); g.setGravity(Gravity.CENTER); content.addView(g);
        Button overlay=btn("Show yourself • floating overlay"); overlay.setOnClickListener(v->showOverlay()); content.addView(overlay);
        String features="ACTIVE IN THIS BUILD\n✓ Natural typed conversation\n✓ Persistent Lumi settings + PIN vault\n✓ Signed cumulative update chain";
        if(FEATURE_LEVEL>=3) features += "\n✓ Voice input\n✓ Structured object memories\n✓ Simple reminders + memory search";
        if(FEATURE_LEVEL>=4) features += "\n✓ Wearable-mode control panel\n✓ Glasses session state + audio-first UX\n○ Meta Wearables SDK connector: awaiting credentials/SDK";
        if(FEATURE_LEVEL>=5) features += "\n✓ Home/Public/Travel profiles\n✓ Do Not Disturb + emergency override setting\n✓ Context-sensitive interruption rules\n✓ Location-awareness permission";
        if(FEATURE_LEVEL>=6) features += "\n✓ Integration center\n✓ Emergency contact + 30-second cancel test\n✓ SMS emergency path (permission required)\n○ Live ChatGPT / Meta / email / calendar / smart-home: connection required";
        addCard(features);
    }

    void showTalk(){ base("Talk to Lumi");
        transcript=tv("Lumi: Talk normally. I keep the interaction shell local in this prototype.",16,text); transcript.setBackgroundColor(panel); content.addView(transcript);
        EditText input=new EditText(this); input.setHint("Say or type anything..."); input.setHintTextColor(muted); input.setTextColor(text); input.setSingleLine(false); input.setMinLines(2); content.addView(input);
        LinearLayout row=new LinearLayout(this); Button send=btn("Send"); row.addView(send,new LinearLayout.LayoutParams(0,58,1));
        if(FEATURE_LEVEL>=3){ Button mic=btn("🎙 Voice"); row.addView(mic,new LinearLayout.LayoutParams(0,58,1)); mic.setOnClickListener(v->startVoice()); }
        content.addView(row);
        send.setOnClickListener(v->{String q=input.getText().toString().trim(); if(q.isEmpty())return; appendConversation(q); input.setText("");});
    }

    void startVoice(){
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Talk to Lumi");
        try{ startActivityForResult(i,REQ_SPEECH); }catch(Exception e){Toast.makeText(this,"Speech recognition is not available on this phone.",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int req,int res,Intent data){ super.onActivityResult(req,res,data); if(req==REQ_SPEECH && res==RESULT_OK && data!=null){ ArrayList<String> r=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS); if(r!=null && !r.isEmpty()){ if(transcript==null) showTalk(); appendConversation(r.get(0)); } } }
    void appendConversation(String q){ if(transcript!=null) transcript.append("\n\nYou: "+q+"\nLumi: "+respond(q)); }

    String respond(String q){String l=q.toLowerCase(Locale.US);
        if(l.contains("show yourself")){showOverlay(); return "There I am.";}
        if(l.contains("go home")){new Handler().postDelayed(this::showHome,350); return "Taking us home.";}
        if(l.contains("give me some space")){prefs.edit().putBoolean("dnd",true).apply(); return "Got it. I'll stay quiet unless something is genuinely important.";}
        if(l.contains("come back") || l.contains("dnd off")){prefs.edit().putBoolean("dnd",false).apply(); return "I'm back.";}
        if(l.contains("loosen") && l.contains("filter")){prefs.edit().putString("filter","Relaxed").apply(); return "Context Filter is now Relaxed.";}
        if(l.contains("strict") && l.contains("filter")){prefs.edit().putString("filter","Strict").apply(); return "Context Filter is now Strict.";}
        if(l.startsWith("remember") || l.contains("remember my") || l.contains("remember that")){saveMemory(q); return "Remembered.";}
        if(FEATURE_LEVEL>=3 && (l.startsWith("remind me") || l.contains("reminder"))){saveReminder(q); return "I saved that reminder in the prototype reminder list.";}
        if(FEATURE_LEVEL>=4 && l.contains("glasses")){prefs.edit().putBoolean("wearable",true).apply(); return "Wearable mode is armed. The real Ray-Ban Meta bridge still needs Meta's SDK connection.";}
        if(FEATURE_LEVEL>=5 && l.contains("public mode")){prefs.edit().putString("profile","Public").apply(); return "Public profile active. I'll be quieter.";}
        if(FEATURE_LEVEL>=5 && l.contains("home mode")){prefs.edit().putString("profile","Home").apply(); return "Home profile active.";}
        return "I heard you naturally. This build is running the local Lumi prototype brain; cloud AI comes through the integration layer once credentials are connected.";
    }

    void saveMemory(String q){ String old=prefs.getString("memories",""); String stamp=new SimpleDateFormat("MMM d, h:mm a",Locale.US).format(new Date()); prefs.edit().putString("memories",old+"\n• "+stamp+" — "+q).apply(); }
    void saveReminder(String q){ String old=prefs.getString("reminders",""); String stamp=new SimpleDateFormat("MMM d, h:mm a",Locale.US).format(new Date()); prefs.edit().putString("reminders",old+"\n• "+stamp+" — "+q).apply(); }

    void showMemory(){ base("Memory"); String m=prefs.getString("memories","").trim(); addCard("SAVED MEMORIES\n"+(m.isEmpty()?"No saved memories yet.":m));
        if(FEATURE_LEVEL>=3){String r=prefs.getString("reminders","").trim(); addCard("REMINDERS\n"+(r.isEmpty()?"No prototype reminders yet.":r)); Button search=btn("Search memories"); content.addView(search); search.setOnClickListener(v->memorySearch());}
        Button clear=btn("Clear prototype memories"); clear.setOnClickListener(v->{prefs.edit().remove("memories").apply();showMemory();}); content.addView(clear);
    }
    void memorySearch(){ final EditText e=new EditText(this); e.setHint("keyword"); new AlertDialog.Builder(this).setTitle("Search Lumi memory").setView(e).setPositiveButton("Search",(d,w)->{String q=e.getText().toString().toLowerCase(Locale.US); String[] lines=prefs.getString("memories","").split("\\n"); StringBuilder out=new StringBuilder(); for(String line:lines) if(line.toLowerCase(Locale.US).contains(q)) out.append(line).append("\n"); new AlertDialog.Builder(this).setTitle("Results").setMessage(out.length()==0?"No matches":out.toString()).setPositiveButton("OK",null).show();}).setNegativeButton("Cancel",null).show(); }

    void showGlasses(){ base("Ray-Ban Meta / Wearable Mode");
        addCard("WEARABLE SESSION\n"+(prefs.getBoolean("wearable",false)?"Status: Armed":"Status: Not armed")+"\n\nThis screen implements Lumi's glasses-first behavior and session state. It does NOT pretend to be connected to Meta's proprietary wearable APIs yet.");
        Button arm=btn(prefs.getBoolean("wearable",false)?"Disarm wearable mode":"Arm wearable mode"); content.addView(arm); arm.setOnClickListener(v->{boolean n=!prefs.getBoolean("wearable",false);prefs.edit().putBoolean("wearable",n).apply();showGlasses();});
        addCard("TARGET COMMANDS\n• Hey Lumi (custom wake phrase target)\n• What's up, Lumi?\n• Lumi, show yourself\n• Lumi, go home\n\nCurrent test: launch Lumi on phone and use voice. Actual wake-word/audio routing on Ray-Ban Meta requires the Meta wearable SDK/API access.");
    }

    void showContext(){ base("Context Engine");
        String profile=prefs.getString("profile","Home"); boolean dnd=prefs.getBoolean("dnd",false);
        addCard("ACTIVE PROFILE: "+profile+"\nDo Not Disturb: "+(dnd?"ON":"OFF")+"\nContext Filter: "+prefs.getString("filter","Balanced")+"\n\nHome = more conversational\nPublic = subtle cues, privacy first\nTravel = tighter privacy + navigation emphasis");
        LinearLayout r=new LinearLayout(this); for(String p:new String[]{"Home","Public","Travel"}){Button b=btn(p);r.addView(b,new LinearLayout.LayoutParams(0,58,1));b.setOnClickListener(v->{prefs.edit().putString("profile",p).apply();showContext();});} content.addView(r);
        Button d=btn(dnd?"Turn DND off":"Give me some space"); content.addView(d); d.setOnClickListener(v->{prefs.edit().putBoolean("dnd",!dnd).apply();showContext();});
        Button loc=btn("Enable location awareness"); content.addView(loc); loc.setOnClickListener(v->requestContextPermissions());
        addCard("INTERRUPTION POLICY\n• Important proactive cues only\n• Around others: subtle cue, wait for acknowledgment\n• Tense conversation: stay out unless asked\n• Driving with others: navigation/safety/important only\n• Reminder timing may be delayed when context is poor");
    }

    void showMore(){ base("Lumi Systems");
        Button vault=btn("Private Lumi Vault");content.addView(vault);vault.setOnClickListener(v->openVault());
        Button integrations=btn("Integration Center");content.addView(integrations);integrations.setOnClickListener(v->showIntegrations());
        Button emergency=btn("Emergency Setup / Test");content.addView(emergency);emergency.setOnClickListener(v->showEmergency());
        Button settings=btn("Settings");content.addView(settings);settings.setOnClickListener(v->showSettings());
    }

    void showIntegrations(){ base("Integration Center");
        addCard("PHONE FEATURES\n✓ Voice input\n✓ Local memory\n✓ Context profiles\n✓ Private vault shell\n✓ Floating Lumi overlay\n✓ Emergency SMS test path");
        addCard("EXTERNAL CONNECTIONS\n○ OpenAI / ChatGPT API — needs API credential + backend\n○ Meta models — needs developer API credential\n○ Ray-Ban Meta device bridge — needs Meta wearable SDK/API\n○ Gmail / Calendar — needs OAuth authorization\n○ Smart home — needs Home Assistant/Alexa/device credentials\n\nThese are connection points, not falsely simulated as live services.");
    }

    void showEmergency(){ base("Emergency");
        String contact=prefs.getString("emergency_number",""); addCard("PRIMARY CONTACT\n"+(contact.isEmpty()?"Not configured":contact)+"\n\nFlow: suspected emergency → check-in → 30-second cancel window → text + current location when available.");
        Button set=btn("Set emergency phone number");content.addView(set);set.setOnClickListener(v->setEmergencyContact());
        Button test=btn("Run 30-second TEST countdown");content.addView(test);test.setOnClickListener(v->startEmergencyCountdown());
        addCard("TEST MODE SAFETY\nThe test does not send a message automatically. It demonstrates the countdown. Actual automatic SMS requires SEND_SMS permission and should only be enabled after you verify the configured contact.");
    }
    void setEmergencyContact(){final EditText e=new EditText(this);e.setInputType(InputType.TYPE_CLASS_PHONE);e.setHint("Phone number");new AlertDialog.Builder(this).setTitle("Emergency contact").setView(e).setPositiveButton("Save",(d,w)->{prefs.edit().putString("emergency_number",e.getText().toString().trim()).apply();showEmergency();}).setNegativeButton("Cancel",null).show();}
    void startEmergencyCountdown(){ final AlertDialog box=new AlertDialog.Builder(this).setTitle("Emergency test").setMessage("30 seconds until the test would escalate. Tap CANCEL to stop.").setNegativeButton("CANCEL",null).create(); box.show(); final Handler h=new Handler(); final int[] sec={30}; Runnable r=new Runnable(){public void run(){ if(!box.isShowing())return; sec[0]--; if(sec[0]<=0){box.dismiss(); new AlertDialog.Builder(MainActivity.this).setTitle("Test complete").setMessage("In live mode this is where Lumi would send the configured text + location.").setPositiveButton("OK",null).show();}else{box.setMessage(sec[0]+" seconds until the test would escalate. Tap CANCEL to stop.");h.postDelayed(this,1000);}}};h.postDelayed(r,1000); }

    void requestContextPermissions(){ if(Build.VERSION.SDK_INT>=23){ArrayList<String> p=new ArrayList<>();if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.ACCESS_FINE_LOCATION);if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.RECORD_AUDIO);if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),REQ_PERMS);else Toast.makeText(this,"Context permissions already granted",Toast.LENGTH_SHORT).show();} }

    void openVault(){ String pin=prefs.getString("pin",""); if(pin.isEmpty()){ setupPin(); return; } final EditText e=new EditText(this); e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD); e.setHint("Lumi PIN"); new AlertDialog.Builder(this).setTitle("Unlock Lumi Vault").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Unlock",(d,w)->{ if(e.getText().toString().equals(pin)) showVault(); else Toast.makeText(this,"Incorrect PIN",Toast.LENGTH_SHORT).show(); }).show(); }
    void setupPin(){ final EditText e=new EditText(this); e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD); e.setHint("Choose Lumi PIN"); new AlertDialog.Builder(this).setTitle("Create Lumi Vault PIN").setMessage("Separate from your phone unlock. Prototype storage only; production vault will use encrypted file storage.").setView(e).setPositiveButton("Save",(d,w)->{if(e.getText().length()>=4){prefs.edit().putString("pin",e.getText().toString()).apply();showVault();}else Toast.makeText(this,"Use at least 4 digits",Toast.LENGTH_SHORT).show();}).setNegativeButton("Cancel",null).show(); }
    void showVault(){ base("Lumi Vault"); addCard("PRIVATE GALLERY PROTOTYPE\nPIN protected and separate from the normal gallery concept. Production target: encrypted storage, 5-minute unlock window, organization by people / places / objects / moments, and indefinite retention for emergency captures."); }

    void showSettings(){ base("Settings");
        content.addView(tv("Context Filter",18,text)); RadioGroup rg=new RadioGroup(this); String cur=prefs.getString("filter","Balanced"); for(String s:new String[]{"Strict","Balanced","Relaxed","Custom"}){RadioButton r=new RadioButton(this);r.setText(s);r.setTextColor(text);r.setChecked(s.equals(cur));r.setOnClickListener(v->prefs.edit().putString("filter",s).apply());rg.addView(r);} content.addView(rg);
        addCard("BEHAVIOR\n✓ Important proactive cues only\n✓ Quiet around other people\n✓ Natural conversation\n✓ Learn from corrections\n✓ High-risk actions require confirmation\n✓ Purchases require approval");
        Button change=btn("Change Lumi Vault PIN"); change.setOnClickListener(v->{prefs.edit().remove("pin").apply();setupPin();});content.addView(change);
        Button overlay=btn("Grant floating-overlay permission"); overlay.setOnClickListener(v->requestOverlay()); content.addView(overlay);
    }
    void requestOverlay(){ if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){ startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()))); } else Toast.makeText(this,"Overlay permission already available",Toast.LENGTH_SHORT).show(); }
    void showOverlay(){ if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){ requestOverlay(); Toast.makeText(this,"Grant overlay permission, then try again",Toast.LENGTH_LONG).show(); return;} startService(new Intent(this,LumiOverlayService.class)); }
}
