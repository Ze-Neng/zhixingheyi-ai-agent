import { BrowserRouter } from "react-router-dom";
import ZhiXingHeYiLayout from "./components/ZhiXingHeYiLayout.tsx";
import { ChatSessionsProvider } from "./contexts/ChatSessionsContext.tsx";

function App() {
  return (
    <BrowserRouter>
      <ChatSessionsProvider>
        <ZhiXingHeYiLayout />
      </ChatSessionsProvider>
    </BrowserRouter>
  );
}

export default App;
